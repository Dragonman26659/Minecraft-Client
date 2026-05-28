package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.*;

/**
 * Handles mining a specific block type.
 *
 * Return values (MineResult) communicate state back to the GOAP planner:
 *   NEEDS_CLOSER  – target found but player is too far; pathfinder should approach
 *   MINING        – actively breaking the target (or clearing obstructions)
 *   DONE          – block broken; dropPos contains the position to collect loot
 *   BLOCKED       – could not mine (blacklisted / hard block / LOS permanently blocked)
 *   SEARCHING     – still scanning for a target block
 *
 * LOS policy
 * ──────────
 * A ray is cast from the player's eyes to the target before each mining attempt.
 * Any solid block in the way is broken first (clearObstruction).
 *
 * The target is immediately blacklisted when:
 *   (a) an obstruction block has destroySpeed < 0  (unbreakable – bedrock, etc.)
 *   (b) an obstruction block does not turn to air within OBSTRUCTION_TIMEOUT ticks
 *       (treats effectively-unbreakable blocks like obsidian as blockers)
 *
 * Blacklisted targets are skipped by findNearest so the miner automatically
 * falls back to the next-nearest candidate.
 */
public class Miner {

    // ── Config ─────────────────────────────────────────────────────────────────
    private static final double MINE_REACH          = 4.0;
    private static final int    MINE_BREAK_TIMEOUT  = 200;
    /** Ticks to spend trying to clear a single obstruction before giving up. */
    private static final int    OBSTRUCTION_TIMEOUT = 120;
    private static final int    BLACKLIST_AFTER     = 2;
    private static final int    SEARCH_RADIUS_STEP  = 8;
    private static final int    MAX_SEARCH_RADIUS   = 64;

    private static final ColorRGBA COLOR_MINE = new ColorRGBA(0x80FF8000);

    // ── Result type ────────────────────────────────────────────────────────────
    public enum MineResult { SEARCHING, NEEDS_CLOSER, MINING, DONE, BLOCKED }

    // ── State ──────────────────────────────────────────────────────────────────
    private final Minecraft mc = Minecraft.getInstance();

    private Block    targetBlockType = null;
    private BlockPos targetPos       = null;
    private boolean  active          = false;
    private int      searchRadius    = 16;

    /** Position where the block was broken – caller should collect drops here. */
    public BlockPos dropPos = null;

    private BlockPos  breakingPos  = null;
    private Direction breakingFace = Direction.UP;
    private int       breakTicks   = 0;

    /** Obstruction currently being cleared for line-of-sight. */
    private BlockPos obstructionPos   = null;
    private int      obstructionTicks = 0;

    private final Map<BlockPos, Integer> failCount = new HashMap<>();
    private final Set<BlockPos>          blacklist = new HashSet<>();

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Begin mining the nearest block of {@code type}. Resets all state. */
    public void start(Block type) {
        targetBlockType  = type;
        targetPos        = null;
        active           = true;
        searchRadius     = 16;
        dropPos          = null;
        breakingPos      = null;
        obstructionPos   = null;
        obstructionTicks = 0;
        failCount.clear();
        blacklist.clear();
    }

    public boolean isActive() { return active; }

    public void stop() {
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        active           = false;
        breakingPos      = null;
        obstructionPos   = null;
        obstructionTicks = 0;
    }

    /**
     * Called every game tick.
     * @return current {@link MineResult} for the GOAP planner.
     */
    public MineResult tick() {
        if (mc.player == null || mc.level == null) return MineResult.BLOCKED;

        // ── Find / re-validate target ──────────────────────────────────────────
        if (targetPos == null || mc.level.getBlockState(targetPos).getBlock() != targetBlockType) {
            resetObstruction();
            targetPos = findNearest(targetBlockType, searchRadius);
            if (targetPos == null) {
                searchRadius += SEARCH_RADIUS_STEP;
                if (searchRadius > MAX_SEARCH_RADIUS) { stop(); return MineResult.BLOCKED; }
                return MineResult.SEARCHING;
            }
        }

        // ── Range check ───────────────────────────────────────────────────────
        double dist = mc.player.position().distanceTo(Vec3.atCenterOf(targetPos));
        if (dist > MINE_REACH) return MineResult.NEEDS_CLOSER;

        // ── Line-of-sight check ───────────────────────────────────────────────
        BlockPos obstruction = findObstruction(targetPos);
        if (obstruction != null) {
            // (a) Immediately reject unbreakable obstructions.
            BlockState obsState = mc.level.getBlockState(obstruction);
            if (obsState.getDestroySpeed(mc.level, obstruction) < 0) {
                System.out.println("[Miner] Unbreakable LOS block " + obstruction
                        + " – blacklisting " + targetPos);
                blacklistTarget();
                return MineResult.BLOCKED;
            }

            // (b) Track how long we've been trying to clear this obstruction.
            if (!obstruction.equals(obstructionPos)) {
                if (obstructionPos != null) mc.gameMode.stopDestroyBlock();
                obstructionPos   = obstruction;
                obstructionTicks = 0;
            } else {
                obstructionTicks++;
                if (obstructionTicks > OBSTRUCTION_TIMEOUT) {
                    System.out.println("[Miner] Obstruction timeout " + obstruction
                            + " – blacklisting " + targetPos);
                    mc.gameMode.stopDestroyBlock();
                    blacklistTarget();
                    return MineResult.BLOCKED;
                }
            }

            clearObstruction(obstruction);
            return MineResult.MINING;
        }

        // LOS is clear – reset any stale obstruction state.
        resetObstruction();

        // ── Mine the target ───────────────────────────────────────────────────
        selectBestTool(targetPos);
        lookAt(Vec3.atCenterOf(targetPos));

        Direction face = getVisibleFace(targetPos);
        if (!targetPos.equals(breakingPos)) {
            mc.gameMode.startDestroyBlock(targetPos, face);
            breakingPos = targetPos;
            breakTicks  = 0;
        } else {
            mc.gameMode.continueDestroyBlock(targetPos, face);
            if (breakTicks++ > MINE_BREAK_TIMEOUT) {
                blacklistTarget();
                return MineResult.BLOCKED;
            }
        }

        // ── Check if block is gone ────────────────────────────────────────────
        if (mc.level.getBlockState(targetPos).isAir()) {
            mc.gameMode.stopDestroyBlock();
            dropPos     = targetPos;
            targetPos   = null;
            breakingPos = null;
            active      = false;
            return MineResult.DONE;
        }

        return MineResult.MINING;
    }

    public void onRender() {
        if (active && targetPos != null) {
            RenderUtil.renderWireFrame(targetPos, COLOR_MINE);
        }
    }

    /** The target block position (for pathfinding approach). */
    public BlockPos getTargetPos() { return targetPos; }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Increments the fail counter for the current target; blacklists it once
     * the threshold is reached.  Always clears targeting and break state.
     */
    private void blacklistTarget() {
        if (targetPos != null) {
            int fails = failCount.merge(targetPos, 1, Integer::sum);
            if (fails >= BLACKLIST_AFTER) {
                blacklist.add(targetPos);
                System.out.println("[Miner] Blacklisted " + targetPos);
            }
        }
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        targetPos        = null;
        breakingPos      = null;
        obstructionPos   = null;
        obstructionTicks = 0;
    }

    private void resetObstruction() {
        obstructionPos   = null;
        obstructionTicks = 0;
    }

    // ── Obstruction clearing ───────────────────────────────────────────────────

    /**
     * Returns the first solid, non-replaceable block between the player's eyes
     * and {@code target}, or {@code null} if the line of sight is clear.
     */
    private BlockPos findObstruction(BlockPos target) {
        if (mc.player == null) return null;
        Vec3 eyes   = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(target);
        Vec3 dir    = center.subtract(eyes).normalize();
        double dist = eyes.distanceTo(center);

        for (double t = 0.5; t < dist - 0.5; t += 0.5) {
            Vec3     sample = eyes.add(dir.scale(t));
            BlockPos bp     = BlockPos.containing(sample);
            if (bp.equals(target)) break;
            BlockState s = mc.level.getBlockState(bp);
            if (!s.isAir() && !s.canBeReplaced()) return bp;
        }
        return null;
    }

    private void clearObstruction(BlockPos pos) {
        selectBestTool(pos);
        lookAt(Vec3.atCenterOf(pos));
        Direction face = getVisibleFace(pos);

        if (!pos.equals(breakingPos)) {
            mc.gameMode.startDestroyBlock(pos, face);
            breakingPos = pos;
        } else {
            mc.gameMode.continueDestroyBlock(pos, face);
        }

        if (mc.level.getBlockState(pos).isAir()) {
            mc.gameMode.stopDestroyBlock();
            breakingPos    = null;
            obstructionPos   = null;
            obstructionTicks = 0;
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private BlockPos findNearest(Block b, int r) {
        if (mc.player == null || mc.level == null) return null;
        BlockPos origin = mc.player.blockPosition();
        BlockPos best   = null;
        double   bestD  = Double.MAX_VALUE;

        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    if (blacklist.contains(p)) continue;
                    if (!mc.level.isLoaded(p)) continue;
                    if (mc.level.getBlockState(p).getBlock() == b) {
                        double d = origin.distSqr(p);
                        if (d < bestD) { bestD = d; best = p; }
                    }
                }
        return best;
    }

    private void selectBestTool(BlockPos p) {
        BlockState s = mc.level.getBlockState(p);
        int best = 0; float max = -1;
        for (int i = 0; i < 9; i++) {
            float spd = mc.player.getInventory().getItem(i).getDestroySpeed(s);
            if (spd > max) { max = spd; best = i; }
        }
        mc.player.getInventory().setSelectedSlot(best);
    }

    private void lookAt(Vec3 t) {
        Vec3 e = mc.player.getEyePosition();
        double dx = t.x - e.x, dy = t.y - e.y, dz = t.z - e.z;
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private Direction getVisibleFace(BlockPos pos) {
        if (mc.player == null) return Direction.UP;
        Vec3 eyes   = mc.player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(pos);
        Direction best    = Direction.UP;
        double    bestDot = -Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            double dot = new Vec3(d.getStepX(), d.getStepY(), d.getStepZ())
                    .dot(eyes.subtract(center).normalize());
            if (dot > bestDot) { bestDot = dot; best = d; }
        }
        return best;
    }
}