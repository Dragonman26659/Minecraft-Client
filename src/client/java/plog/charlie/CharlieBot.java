package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.*;

public class CharlieBot {

    // ── Config ─────────────────────────────────────────────────────────────────
    boolean canMine = true;

    private static final double ARRIVE_DIST        = 0.5;
    private static final int    MAX_NODES          = 8192;
    private static final int    MOVE_TICKS         = 1;
    private static final int    BREAK_TIMEOUT      = 100;
    private static final int    BLACKLIST_AFTER    = 2;
    private static final int    MINE_BREAK_TIMEOUT = 200;

    private static final double PICKUP_SCAN_RADIUS  = 8.0;
    private static final double PICKUP_COLLECT_DIST = 1.0;
    private static final int    PICKUP_TIMEOUT       = 80;

    private static final ColorRGBA COLOR_GOTO   = new ColorRGBA(0x8000FFFF);
    private static final ColorRGBA COLOR_MINE   = new ColorRGBA(0x80FF8000);
    private static final ColorRGBA COLOR_PICKUP = new ColorRGBA(0x8000FF80);

    // ── Internal state ─────────────────────────────────────────────────────────
    private final Minecraft mc = Minecraft.getInstance();

    private List<BlockPos> currentPath = new ArrayList<>();
    private BlockPos       gotoGoal    = null;
    private int            pathIndex   = 0;
    private int            moveTicker  = 0;
    private boolean        goToActive  = false;

    private BlockPos  breakingPos  = null;
    private Direction breakingFace = Direction.SOUTH;
    private int       breakTicks   = 0;

    private BlockPos        mineTarget       = null;
    private Block           mineBlockType    = null;
    private boolean         mineActive       = false;
    private int             mineSearch       = 0;
    private final Map<BlockPos, Integer> mineFailCount = new HashMap<>();
    private final Set<BlockPos>          mineBlacklist = new HashSet<>();

    private BlockPos  activeBreakPos   = null;
    private Direction activeBreakFace  = Direction.UP;
    private int       activeBreakTicks = 0;

    private Vec3    pickupOrigin  = null;
    private boolean pickupActive  = false;
    private int     pickupTicks   = 0;

    // ── Public API ─────────────────────────────────────────────────────────────

    public void GoTo(BlockPos position) {
        if (mc.player == null || mc.level == null) return;
        List<BlockPos> path = findPath(mc.player.blockPosition(), position);
        if (path == null || path.isEmpty()) return;

        currentPath = path;
        gotoGoal    = position;
        pathIndex   = 0;
        moveTicker  = 0;
        goToActive  = true;
        breakingPos = null;
    }

    public void Mine(Block blockType) {
        if (mc.player == null || mc.level == null) return;
        mineBlockType = blockType;
        mineTarget    = null;
        mineSearch    = 0;
        mineActive    = true;
        mineFailCount.clear();
        mineBlacklist.clear();
        activeBreakPos = null;
    }

    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (pickupActive) { tickPickup(); return; }
        if (goToActive) tickGoTo();
        if (mineActive)  tickMine();
    }

    public void onRender() {
        if (mc.player == null || mc.level == null) return;
        if (goToActive && gotoGoal != null) {
            RenderUtil.renderWireFrame(gotoGoal, COLOR_GOTO);
            RenderUtil.renderTracer(Vec3.atCenterOf(gotoGoal), COLOR_GOTO);
        }
        if (mineActive && mineTarget != null) {
            RenderUtil.renderWireFrame(mineTarget, COLOR_MINE);
            RenderUtil.renderTracer(Vec3.atCenterOf(mineTarget), COLOR_MINE);
        }
    }

    public void stop() {
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        goToActive = false; mineActive = false; pickupActive = false;
        releaseAllKeys();
    }

    // ── Logic ──────────────────────────────────────────────────────────────────

    private void tickGoTo() {
        Player player = mc.player;
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            stopGoTo(true);
            return;
        }

        BlockPos waypoint = currentPath.get(pathIndex);
        BlockPos headPos = waypoint.above();

        // Check for basic body obstructions
        boolean feetBlocked = isBreakableObstacle(waypoint, mc.level.getBlockState(waypoint));
        boolean headBlocked = isBreakableObstacle(headPos, mc.level.getBlockState(headPos));

        if (feetBlocked || headBlocked) {
            breakPathBlock(feetBlocked ? waypoint : headPos);
            return;
        }

        Vec3 pos = player.position();
        double currentY = Math.floor(pos.y);

        // HEIGHT AWARENESS: Jumping Up (requires 3 blocks)
        if (waypoint.getY() > currentY + 0.5) {
            BlockPos jumpClearancePos = player.blockPosition().above(2);
            if (isBreakableObstacle(jumpClearancePos, mc.level.getBlockState(jumpClearancePos))) {
                breakPathBlock(jumpClearancePos);
                return;
            }
            if (player.onGround()) player.jumpFromGround();
        }

        // HEIGHT AWARENESS: Stepping Down (requires 3 blocks to enter the hole)
        if (waypoint.getY() < currentY - 0.5) {
            // Check the block 2 units above the waypoint (the 'ceiling' of the hole)
            BlockPos holeCeiling = waypoint.above(2);
            if (isBreakableObstacle(holeCeiling, mc.level.getBlockState(holeCeiling))) {
                breakPathBlock(holeCeiling);
                return;
            }
        }

        // Normal Movement
        if (breakingPos != null) { mc.gameMode.stopDestroyBlock(); breakingPos = null; }

        Vec3 wpCenter = Vec3.atBottomCenterOf(waypoint);
        double dx = wpCenter.x - pos.x;
        double dz = wpCenter.z - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < ARRIVE_DIST && Math.abs(waypoint.getY() - pos.y) < 1.2) {
            pathIndex++;
            return;
        }

        if (moveTicker++ % MOVE_TICKS == 0) {
            player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
            mc.options.keyUp.setDown(true);
        }
    }

    private void tickMine() {
        Player player = mc.player;
        if (mineTarget == null || mc.level.getBlockState(mineTarget).getBlock() != mineBlockType) {
            mineTarget = findNearest(mineBlockType, 16 + mineSearch);
            if (mineTarget == null) { mineSearch += 8; return; }
        }

        double dist = player.position().distanceTo(Vec3.atCenterOf(mineTarget));
        if (dist > 4.0) {
            if (!goToActive || !mineTarget.equals(gotoGoal)) GoTo(mineTarget);
            tickGoTo();
            return;
        }

        if (goToActive) stopGoTo(false);

        selectBestTool(mineTarget);
        lookAt(Vec3.atCenterOf(mineTarget));
        handleBreaking(mineTarget, true);
    }

    private void tickPickup() {
        if (pickupOrigin == null) { pickupActive = false; return; }
        List<ItemEntity> drops = getNearbyDrops(pickupOrigin, PICKUP_SCAN_RADIUS);

        if (drops.isEmpty() || pickupTicks++ > PICKUP_TIMEOUT) {
            stopPickup();
            return;
        }

        ItemEntity closest = drops.stream()
                .min(Comparator.comparingDouble(i -> mc.player.distanceToSqr(i)))
                .orElse(null);

        if (closest != null && mc.player.distanceTo(closest) > PICKUP_COLLECT_DIST) {
            BlockPos dropPos = closest.blockPosition();
            if (!goToActive || !dropPos.equals(gotoGoal)) GoTo(dropPos);
            tickGoTo();
        } else {
            releaseAllKeys();
        }
    }

    // ── Pathfinder (2-High + 3-High Jump Awareness) ────────────────────────────

    private List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<BlockPos, Integer> gScores = new HashMap<>();
        Map<BlockPos, BlockPos> parentMap = new HashMap<>();

        open.add(new Node(start, 0, heuristic(start, goal)));
        gScores.put(start, 0);

        while (!open.isEmpty() && gScores.size() < MAX_NODES) {
            BlockPos curr = open.poll().pos;
            if (curr.closerThan(goal, 1.5)) return reconstructPath(parentMap, curr);

            for (BlockPos next : getNeighbors(curr)) {
                int tent = gScores.get(curr) + 1;
                if (tent < gScores.getOrDefault(next, Integer.MAX_VALUE)) {
                    gScores.put(next, tent);
                    parentMap.put(next, curr);
                    open.add(new Node(next, tent, heuristic(next, goal)));
                }
            }
        }
        return null;
    }

    private List<BlockPos> getNeighbors(BlockPos p) {
        List<BlockPos> neighbors = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos n = p.relative(d).offset(0, dy, 0);
                if (isPassable(p, n, dy)) neighbors.add(n);
            }
        }
        return neighbors;
    }

    /**
     * HEIGHT AWARENESS:
     * - Feet (n) must be breakable/air.
     * - Head (n.above()) must be breakable/air.
     * - If dy > 0 (Jumping), the block 2 above current (p.above(2)) must be breakable/air (3-block rule).
     */
    private boolean isPassable(BlockPos current, BlockPos next, int dy) {
        if (mc.level == null) return false;

        // 1. Floor check (must stand on something at the destination)
        if (mc.level.getBlockState(next.below()).isAir()) return false;

        // 2. Standard Body check (Destination must fit 2 blocks)
        if (!canPass(next) || !canPass(next.above())) return false;

        // 3. JUMPING: Must have 3 blocks of clearance above current position to jump
        if (dy > 0) {
            if (!canPass(current.above(2))) return false;
        }

        // 4. MINING DOWN: Must have 3 blocks of clearance above the landing spot
        // to walk forward into the hole. (Clears the 'lintel' block)
        if (dy < 0) {
            BlockPos lintel = next.above(2);
            if (!canPass(lintel)) return false;
        }

        return true;
    }

    private boolean canPass(BlockPos p) {
        BlockState s = mc.level.getBlockState(p);
        if (s.isAir() || s.canBeReplaced()) return true;
        return canMine && s.getDestroySpeed(mc.level, p) >= 0;
    }

    // ── Utils ──────────────────────────────────────────────────────────────────

    private void breakPathBlock(BlockPos pos) {
        releaseAllKeys();
        selectBestTool(pos);
        lookAt(Vec3.atCenterOf(pos));
        handleBreaking(pos, false);
    }

    private void handleBreaking(BlockPos pos, boolean isMineTask) {
        Direction face = getVisibleFace(pos);
        if (!pos.equals(isMineTask ? activeBreakPos : breakingPos)) {
            mc.gameMode.startDestroyBlock(pos, face);
            if (isMineTask) { activeBreakPos = pos; activeBreakTicks = 0; }
            else { breakingPos = pos; breakTicks = 0; }
        } else {
            mc.gameMode.continueDestroyBlock(pos, face);
            if (isMineTask && activeBreakTicks++ > MINE_BREAK_TIMEOUT) { mineBlacklist.add(pos); mineTarget = null; }
            if (!isMineTask && breakTicks++ > BREAK_TIMEOUT) pathIndex++;
        }

        if (mc.level.getBlockState(pos).isAir()) {
            mc.gameMode.stopDestroyBlock();
            if (isMineTask) { pickupOrigin = Vec3.atCenterOf(pos); pickupActive = true; mineTarget = null; }
            else { breakingPos = null; }
        }
    }

    private boolean isBreakableObstacle(BlockPos p, BlockState s) {
        return !s.isAir() && !s.canBeReplaced() && s.getDestroySpeed(mc.level, p) >= 0;
    }

    private void stopGoTo(boolean announce) {
        goToActive = false; gotoGoal = null; currentPath.clear(); releaseAllKeys();
        if (announce) System.out.println("[charlie] Arrived.");
    }

    private void stopPickup() {
        pickupActive = false; pickupOrigin = null; pickupTicks = 0; stopGoTo(false);
    }

    private void lookAt(Vec3 t) {
        Vec3 e = mc.player.getEyePosition();
        double dx = t.x - e.x, dy = t.y - e.y, dz = t.z - e.z;
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))));
    }

    private Direction getVisibleFace(BlockPos pos) {

        if (mc.player == null) return Direction.UP;

        Vec3 eyes = mc.player.getEyePosition();

        Vec3 center = Vec3.atCenterOf(pos);

        Direction best = Direction.UP;

        double bestDot = -Double.MAX_VALUE;

        for (Direction d : Direction.values()) {

            double dot = new Vec3(d.getStepX(), d.getStepY(), d.getStepZ())

                    .dot(eyes.subtract(center).normalize());

            if (dot > bestDot) { bestDot = dot; best = d; }

        }

        return best;

    }

    private void selectBestTool(BlockPos p) {
        BlockState s = mc.level.getBlockState(p);
        int best = 0; float max = -1;
        for (int i=0; i<9; i++) {
            float spd = mc.player.getInventory().getItem(i).getDestroySpeed(s);
            if (spd > max) { max = spd; best = i; }
        }
        mc.player.getInventory().setSelectedSlot(best);
    }

    private void releaseAllKeys() {
        mc.options.keyUp.setDown(false);
    }

    private BlockPos findNearest(Block b, int r) {
        BlockPos o = mc.player.blockPosition();
        BlockPos best = null; double d = Double.MAX_VALUE;
        for (int x=-r; x<=r; x++) for (int y=-r; y<=r; y++) for (int z=-r; z<=r; z++) {
            BlockPos p = o.offset(x,y,z);
            if (mineBlacklist.contains(p)) continue;
            if (mc.level.getBlockState(p).getBlock() == b) {
                double dist = o.distSqr(p);
                if (dist < d) { d = dist; best = p; }
            }
        }
        return best;
    }

    private int heuristic(BlockPos a, BlockPos b) { return a.distManhattan(b); }
    private List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> m, BlockPos c) {
        LinkedList<BlockPos> p = new LinkedList<>();
        while (c != null) { p.addFirst(c); c = m.get(c); }
        return p;
    }

    private static class Node {
        BlockPos pos; int g, f;
        Node(BlockPos p, int g, int h) { this.pos = p; this.g = g; this.f = g + h; }
    }

    private List<ItemEntity> getNearbyDrops(Vec3 o, double r) {
        return mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(o.subtract(r,r,r), o.add(r,r,r)), i -> !i.getItem().isEmpty());
    }
}