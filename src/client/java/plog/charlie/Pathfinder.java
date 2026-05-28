package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.*;

/**
 * A* pathfinder with environment awareness.
 *
 * Features
 * ────────
 * • 8-Way (Diagonal) pathfinding with corner-cutting prevention.
 * • Auto-repaths if the final node is reached but the goal hasn't been.
 * • Opens doors, fence gates, and trapdoors automatically.
 * • Places blocks to bridge gaps.
 * • Mines path obstructions; prefers free routes via A* cost penalties.
 * • 2-high body clearance, 3-high jump clearance, step-down lintel checks.
 */
public class Pathfinder {

    // ── Config ─────────────────────────────────────────────────────────────────
    private static final int    MAX_NODES      = 8192;
    private static final int    MOVE_TICKS     = 1;
    private static final int    BREAK_TIMEOUT  = 30;
    private static final double ARRIVE_DIST    = 0.5;
    private static final int    MAX_PILLAR_DEPTH = 16;

    // Movement base costs (scaled x10 for precision)
    private static final int COST_STRAIGHT = 10;
    private static final int COST_DIAGONAL = 14;

    // Extra A* cost penalties (scaled relative to base costs)
    private static final int MINE_PENALTY  = 500;
    private static final int PLACE_PENALTY = 300;

    private static final ColorRGBA COLOR_GOTO = new ColorRGBA(0x8000FFFF);

    // ── State ──────────────────────────────────────────────────────────────────
    private final Minecraft mc = Minecraft.getInstance();

    private Placer placer = null;

    private List<BlockPos> currentPath = new ArrayList<>();
    private BlockPos       gotoGoal    = null;
    private int            pathIndex   = 0;
    private int            moveTicker  = 0;
    private boolean        active      = false;

    private BlockPos breakingPos = null;
    private int      breakTicks  = 0;
    private BlockPos pillarTargetPos = null;

    // ── Public API ─────────────────────────────────────────────────────────────

    public void setPlacer(Placer p) { this.placer = p; }

    public void goTo(BlockPos goal) {
        if (mc.player == null || mc.level == null) return;
        List<BlockPos> path = findPath(mc.player.blockPosition(), goal);

        if (path == null || path.isEmpty()) {
            stop(false);
            return;
        }

        currentPath    = path;
        gotoGoal       = goal;
        pathIndex      = 0;
        moveTicker     = 0;
        active         = true;
        breakingPos    = null;
        pillarTargetPos = null;
    }

    public boolean isActive() { return active; }

    public void tick() {
        if (!active || mc.player == null || mc.level == null) return;

        // ── Auto-Repathing Logic ───────────────────────────────────────────────
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            if (gotoGoal != null && !mc.player.blockPosition().closerThan(gotoGoal, 1.5)) {
                System.out.println("[Pathfinder] Goal not reached. Re-evaluating path...");
                BlockPos cachedGoal = gotoGoal;
                stop(false); // Clean up state
                goTo(cachedGoal); // Fire again
            } else {
                stop(true);
            }
            return;
        }

        BlockPos waypoint = currentPath.get(pathIndex);
        BlockPos headPos  = waypoint.above();

        // ── Open interactive blocks (doors, gates, trapdoors) ─────────────────
        if (tryOpenInteractable(waypoint) || tryOpenInteractable(headPos)) return;

        // ── Bridge / pillar: ensure there is a floor under the waypoint ───────
        if (mc.level.getBlockState(waypoint.below()).isAir()) {
            handleMissingFloor(waypoint);
            return;
        }

        // ── Clear body obstructions ────────────────────────────────────────────
        boolean feetBlocked = isBreakableObstacle(waypoint, mc.level.getBlockState(waypoint));
        boolean headBlocked = isBreakableObstacle(headPos,  mc.level.getBlockState(headPos));

        if (feetBlocked || headBlocked) {
            breakPathBlock(feetBlocked ? waypoint : headPos);
            return;
        }

        Vec3   pos   = mc.player.position();
        double currY = Math.floor(pos.y);

        // ── Jump clearance ─────────────────────────────────────────────────────
        if (waypoint.getY() > currY + 0.5) {
            BlockPos jumpClear = mc.player.blockPosition().above(2);
            if (isBreakableObstacle(jumpClear, mc.level.getBlockState(jumpClear))) {
                breakPathBlock(jumpClear);
                return;
            }
            if (mc.player.onGround()) mc.player.jumpFromGround();
        }

        // ── Step-down lintel ───────────────────────────────────────────────────
        if (waypoint.getY() < currY - 0.5) {
            BlockPos lintel = waypoint.above(2);
            if (isBreakableObstacle(lintel, mc.level.getBlockState(lintel))) {
                breakPathBlock(lintel);
                return;
            }
        }

        // ── Normal movement ────────────────────────────────────────────────────
        if (breakingPos != null)  { mc.gameMode.stopDestroyBlock(); breakingPos = null; }
        pillarTargetPos = null;

        Vec3   wpCenter = Vec3.atBottomCenterOf(waypoint);
        double dx       = wpCenter.x - pos.x;
        double dz       = wpCenter.z - pos.z;
        double dist     = Math.sqrt(dx * dx + dz * dz);

        if (dist < ARRIVE_DIST && Math.abs(waypoint.getY() - pos.y) < 1.2) {
            pathIndex++;
            return;
        }

        if (moveTicker++ % MOVE_TICKS == 0) {
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
            mc.options.keyUp.setDown(true);
        }
    }

    public void onRender() {
        if (active && gotoGoal != null) {
            RenderUtil.renderWireFrame(gotoGoal, COLOR_GOTO);
        }
    }

    public void stop(boolean arrived) {
        active = false;
        gotoGoal = null;
        currentPath.clear();
        releaseAllKeys();
        if (breakingPos != null) { mc.gameMode.stopDestroyBlock(); breakingPos = null; }
        pillarTargetPos = null;
        if (arrived) System.out.println("[Pathfinder] Arrived.");
    }

    // ── Door / gate / trapdoor handling ───────────────────────────────────────

    private boolean tryOpenInteractable(BlockPos pos) {
        if (mc.player == null || mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);

        boolean isDoor     = state.getBlock() instanceof DoorBlock;
        boolean isGate     = state.getBlock() instanceof FenceGateBlock;
        boolean isTrapdoor = state.getBlock() instanceof TrapDoorBlock;

        if (!isDoor && !isGate && !isTrapdoor) return false;

        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN))
            return false;

        lookAt(Vec3.atCenterOf(pos));
        releaseAllKeys();
        Direction      face = getVisibleFace(pos);
        BlockHitResult hit  = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
        mc.gameMode.useItemOn(mc.player, mc.player.getUsedItemHand(), hit);
        return true;
    }

    // ── Gap / pillar handling ─────────────────────────────────────────────────

    private void handleMissingFloor(BlockPos waypoint) {
        if (placer == null || mc.player == null || mc.level == null) {
            pathIndex++;
            return;
        }

        int slot = findBlockSlot();
        if (slot == -1) {
            pathIndex++;
            return;
        }

        BlockPos surface = null;
        for (int depth = 1; depth <= MAX_PILLAR_DEPTH; depth++) {
            BlockPos check = waypoint.below(depth);
            if (!mc.level.getBlockState(check).isAir()) {
                surface = check;
                break;
            }
        }

        if (surface == null) {
            System.out.println("[Pathfinder] Gap too deep at " + waypoint + " – skipping");
            pathIndex++;
            return;
        }

        BlockPos placeTarget = surface.above();
        for (int depth = 1; depth < (waypoint.getY() - surface.getY()); depth++) {
            BlockPos candidate = surface.above(depth);
            if (mc.level.getBlockState(candidate).isAir()) {
                placeTarget = candidate;
                break;
            }
        }

        if (!mc.level.getBlockState(placeTarget).isAir()) {
            pillarTargetPos = null;
            return;
        }

        BlockPos supportPos = placeTarget.below();

        releaseAllKeys();
        lookAt(Vec3.atCenterOf(placeTarget));
        mc.player.getInventory().setSelectedSlot(slot);

        Vec3           hitVec    = Vec3.atCenterOf(supportPos);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, supportPos, false);
        mc.gameMode.useItemOn(mc.player, mc.player.getUsedItemHand(), hitResult);

        pillarTargetPos = placeTarget;
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    // ── Pathfinding ────────────────────────────────────────────────────────────

    private List<BlockPos> findPath(BlockPos start, BlockPos goal) {
        PriorityQueue<Node>     open    = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        Map<BlockPos, Integer>  gScores = new HashMap<>();
        Map<BlockPos, BlockPos> parent  = new HashMap<>();

        open.add(new Node(start, 0, heuristic(start, goal)));
        gScores.put(start, 0);

        BlockPos bestReachable = start;
        int      bestH         = heuristic(start, goal);

        while (!open.isEmpty() && gScores.size() < MAX_NODES) {
            Node     node = open.poll();
            BlockPos curr = node.pos;

            int h = heuristic(curr, goal);
            if (h < bestH) { bestH = h; bestReachable = curr; }

            if (curr.closerThan(goal, 1.5)) return reconstructPath(parent, curr);

            for (int[] step : getNeighborSteps(curr)) {
                BlockPos next = curr.offset(step[0], step[1], step[2]);
                int tent      = gScores.get(curr) + step[3];
                if (tent < gScores.getOrDefault(next, Integer.MAX_VALUE)) {
                    gScores.put(next, tent);
                    parent.put(next, curr);
                    open.add(new Node(next, tent, heuristic(next, goal)));
                }
            }
        }

        return bestReachable.equals(start) ? null : reconstructPath(parent, bestReachable);
    }

    /** Returns {dx, dy, dz, cost} for each valid 8-way neighbour. */
    private List<int[]> getNeighborSteps(BlockPos p) {
        List<int[]> steps = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip self

                boolean isDiagonal = (dx != 0 && dz != 0);

                // Prevent clipping/getting stuck on corners when moving diagonally.
                // Ensures at least the feet level of BOTH orthogonal adjacent blocks are clear.
                if (isDiagonal && mc.level != null) {
                    BlockState ortho1 = mc.level.getBlockState(p.offset(dx, 0, 0));
                    BlockState ortho2 = mc.level.getBlockState(p.offset(0, 0, dz));
                    if (!isPassable(ortho1) || !isPassable(ortho2)) {
                        continue;
                    }
                }

                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n    = p.offset(dx, dy, dz);
                    int baseCost  = isDiagonal ? COST_DIAGONAL : COST_STRAIGHT;
                    int cost      = stepCost(p, n, dy, baseCost);

                    if (cost >= 0) steps.add(new int[]{ dx, dy, dz, cost });
                }
            }
        }
        return steps;
    }

    private int stepCost(BlockPos current, BlockPos next, int dy, int baseCost) {
        if (mc.level == null) return -1;

        BlockState floor = mc.level.getBlockState(next.below());
        BlockState feet  = mc.level.getBlockState(next);
        BlockState head  = mc.level.getBlockState(next.above());

        // ── Floor check ───────────────────────────────────────────────────────
        boolean floorSolid     = !floor.isAir() && !floor.canBeReplaced();
        boolean floorBridgeable = !floorSolid && placer != null && findBlockSlot() != -1
                && canBridgeBelow(next);
        if (!floorSolid && !floorBridgeable) return -1;

        // ── Body clearance ─────────────────────────────────────────────────────
        boolean feetPassable = isPassable(feet);
        boolean headPassable = isPassable(head);
        boolean feetMineable = isMineableBlock(next, feet);
        boolean headMineable = isMineableBlock(next.above(), head);
        if (!feetPassable && !feetMineable) return -1;
        if (!headPassable && !headMineable) return -1;

        // ── Jump clearance ─────────────────────────────────────────────────────
        if (dy > 0) {
            BlockState jc = mc.level.getBlockState(current.above(2));
            if (!isPassable(jc) && !isMineableBlock(current.above(2), jc)) return -1;
        }

        // ── Step-down lintel ───────────────────────────────────────────────────
        if (dy < 0) {
            BlockState lintel = mc.level.getBlockState(next.above(2));
            if (!isPassable(lintel) && !isMineableBlock(next.above(2), lintel)) return -1;
        }

        // ── Cost ──────────────────────────────────────────────────────────────
        int cost = baseCost;
        if (!feetPassable || !headPassable)                                          cost += MINE_PENALTY;
        if (dy > 0 && !isPassable(mc.level.getBlockState(current.above(2))))         cost += MINE_PENALTY;
        if (dy < 0 && !isPassable(mc.level.getBlockState(next.above(2))))            cost += MINE_PENALTY;
        if (floorBridgeable)                                                         cost += PLACE_PENALTY;

        return cost;
    }

    private boolean canBridgeBelow(BlockPos pos) {
        for (int d = 1; d <= MAX_PILLAR_DEPTH; d++) {
            BlockPos check = pos.below(d);
            if (!mc.level.isLoaded(check)) return false;
            if (!mc.level.getBlockState(check).isAir()) return true;
        }
        return false;
    }

    // ── Block-breaking helpers ─────────────────────────────────────────────────

    private void breakPathBlock(BlockPos pos) {
        releaseAllKeys();
        selectBestTool(pos);
        lookAt(Vec3.atCenterOf(pos));

        Direction face = getVisibleFace(pos);
        if (!pos.equals(breakingPos)) {
            mc.gameMode.startDestroyBlock(pos, face);
            breakingPos = pos;
            breakTicks  = 0;
        } else {
            mc.gameMode.continueDestroyBlock(pos, face);
            if (breakTicks++ > BREAK_TIMEOUT) pathIndex++;
        }

        if (mc.level.getBlockState(pos).isAir()) {
            mc.gameMode.stopDestroyBlock();
            breakingPos = null;
        }
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private boolean isBreakableObstacle(BlockPos p, BlockState s) {
        if (s.getBlock() instanceof DoorBlock)      return false;
        if (s.getBlock() instanceof FenceGateBlock) return false;
        if (s.getBlock() instanceof TrapDoorBlock)  return false;
        return !s.isAir() && !s.canBeReplaced() && s.getDestroySpeed(mc.level, p) >= 0;
    }

    private boolean isPassable(BlockState s) {
        return s.isAir() || s.canBeReplaced()
                || s.getBlock() instanceof DoorBlock
                || s.getBlock() instanceof FenceGateBlock
                || s.getBlock() instanceof TrapDoorBlock;
    }

    private boolean isMineableBlock(BlockPos p, BlockState s) {
        if (s.getBlock() instanceof DoorBlock)      return false;
        if (s.getBlock() instanceof FenceGateBlock) return false;
        if (s.getBlock() instanceof TrapDoorBlock)  return false;
        return !s.isAir() && s.getDestroySpeed(mc.level, p) >= 0;
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

    private void selectBestTool(BlockPos p) {
        BlockState s = mc.level.getBlockState(p);
        int best = 0; float max = -1;
        for (int i = 0; i < 9; i++) {
            float spd = mc.player.getInventory().getItem(i).getDestroySpeed(s);
            if (spd > max) { max = spd; best = i; }
        }
        mc.player.getInventory().setSelectedSlot(best);
    }

    private void releaseAllKeys() { mc.options.keyUp.setDown(false); }

    private int heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int min = Math.min(dx, dz);
        int max = Math.max(dx, dz);
        return (COST_DIAGONAL * min + COST_STRAIGHT * (max - min)) + COST_STRAIGHT * dy;
    }

    private List<BlockPos> reconstructPath(Map<BlockPos, BlockPos> m, BlockPos c) {
        LinkedList<BlockPos> p = new LinkedList<>();
        while (c != null) { p.addFirst(c); c = m.get(c); }
        return p;
    }

    private static class Node {
        BlockPos pos; int g, f;
        Node(BlockPos p, int g, int h) { pos = p; this.g = g; f = g + h; }
    }
}