package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.*;

/**
 * GOAP-based top-level controller for CharlieBot.
 *
 * ── How goals work ───────────────────────────────────────────────────────────
 * Goals are stored in a true LIFO stack (ArrayDeque used as a stack).
 * The top of the stack is the currently executing goal.  Sub-goals are pushed
 * on top; when they complete they are popped and the parent resumes.
 *
 * ── Public command API ───────────────────────────────────────────────────────
 *   bot.goTo(pos)              – navigate to a position
 *   bot.mine(block)            – find and mine the nearest block of that type
 *   bot.place(pos, block)      – place a specific block at a position
 *   bot.placeAny(pos)          – place any block at a position
 *   bot.stop()                 – clear everything
 */
public class CharlieBot {

    // ── Sub-systems ────────────────────────────────────────────────────────────
    private final Pathfinder pathfinder = new Pathfinder();
    private final Miner      miner      = new Miner();
    private final Placer     placer     = new Placer();

    // ── GOAP stack ─────────────────────────────────────────────────────────────
    private final Deque<Goal> goalStack = new ArrayDeque<>();

    private final Minecraft mc = Minecraft.getInstance();

    // ── Pickup config ──────────────────────────────────────────────────────────
    private static final double PICKUP_SCAN_RADIUS  = 8.0;
    private static final double PICKUP_COLLECT_DIST = 1.0;
    private static final int    PICKUP_TIMEOUT      = 80;
    private static final ColorRGBA COLOR_PICKUP = new ColorRGBA(0x8000FF80);

    // ── Init ───────────────────────────────────────────────────────────────────

    public CharlieBot() {
        // Wire the placer into the pathfinder so it can bridge gaps autonomously.
        pathfinder.setPlacer(placer);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Public command interface
    // ══════════════════════════════════════════════════════════════════════════

    public void goTo(BlockPos pos)          { clearGoals(); pushGoal(new GoToGoal(pos)); }
    public void mine(Block type)            { clearGoals(); pushGoal(new MineGoal(type)); }
    public void place(BlockPos pos, Block b){ clearGoals(); pushGoal(new PlaceGoal(pos, b)); }
    public void placeAny(BlockPos pos)      { clearGoals(); pushGoal(new PlaceGoal(pos, null)); }

    public void stop() {
        clearGoals();
        pathfinder.stop(false);
        miner.stop();
        placer.stop();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Block-break notification  (call this whenever any block is confirmed gone)
    // ══════════════════════════════════════════════════════════════════════════


    // ══════════════════════════════════════════════════════════════════════════
    // Tick / Render
    // ══════════════════════════════════════════════════════════════════════════

    public void onTick() {
        if (mc.player == null || mc.level == null) return;
        if (goalStack.isEmpty()) return;

        Goal current = goalStack.peek();
        GoalStatus status = current.tick(this);

        switch (status) {
            case DONE   -> popGoal();
            case FAILED -> { System.out.println("[GOAP] Goal failed: " + current); popGoal(); }
            case RUNNING -> { /* keep going */ }
        }
    }

    public void onRender() {
        pathfinder.onRender();
        miner.onRender();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Goal stack helpers
    // ══════════════════════════════════════════════════════════════════════════

    void pushGoal(Goal g)  { goalStack.push(g); }
    private void popGoal() { if (!goalStack.isEmpty()) goalStack.pop(); }
    private void clearGoals() { goalStack.clear(); }

    // ══════════════════════════════════════════════════════════════════════════
    // Status / interface
    // ══════════════════════════════════════════════════════════════════════════

    enum GoalStatus { RUNNING, DONE, FAILED }

    interface Goal {
        GoalStatus tick(CharlieBot bot);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GoToGoal
    // ══════════════════════════════════════════════════════════════════════════

    class GoToGoal implements Goal {
        private final BlockPos target;
        private boolean started = false;

        GoToGoal(BlockPos target) { this.target = target; }

        @Override
        public GoalStatus tick(CharlieBot bot) {
            if (!started) { pathfinder.goTo(target); started = true; }
            pathfinder.tick();
            return pathfinder.isActive() ? GoalStatus.RUNNING : GoalStatus.DONE;
        }

        @Override public String toString() { return "GoToGoal(" + target + ")"; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MineGoal
    // ══════════════════════════════════════════════════════════════════════════

    class MineGoal implements Goal {
        private final Block blockType;
        private boolean started = false;

        MineGoal(Block blockType) { this.blockType = blockType; }

        @Override
        public GoalStatus tick(CharlieBot bot) {
            if (!started) { miner.start(blockType); started = true; }

            Miner.MineResult result = miner.tick();

            switch (result) {
                case NEEDS_CLOSER -> {
                    BlockPos target = miner.getTargetPos();
                    if (target != null && !pathfinder.isActive()) {
                        bot.pushGoal(new GoToGoal(target));
                    } else {
                        pathfinder.tick();
                    }
                }
                case DONE -> {
                    // Notify subsystems about the broken block before doing anything else.
                    // This lets the pathfinder replan before the PickupGoal starts moving.
                    if (miner.dropPos != null) {
                        bot.pushGoal(new PickupGoal(Vec3.atCenterOf(miner.dropPos)));
                    }
                    return GoalStatus.DONE;
                }
                case BLOCKED -> { return GoalStatus.FAILED; }
                case MINING, SEARCHING -> { /* continue */ }
            }
            return GoalStatus.RUNNING;
        }

        @Override public String toString() { return "MineGoal(" + blockType + ")"; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PlaceGoal
    // ══════════════════════════════════════════════════════════════════════════

    class PlaceGoal implements Goal {
        private final BlockPos pos;
        private final Block    block;
        private boolean started = false;

        PlaceGoal(BlockPos pos, Block block) { this.pos = pos; this.block = block; }

        @Override
        public GoalStatus tick(CharlieBot bot) {
            if (!started) {
                if (block != null) placer.placeSpecific(pos, block);
                else               placer.placeAny(pos);
                started = true;
            }

            Placer.PlaceResult result = placer.tick();

            switch (result) {
                case NEEDS_CLOSER -> {
                    if (!pathfinder.isActive()) bot.pushGoal(new GoToGoal(pos));
                    else                        pathfinder.tick();
                }
                case NEEDS_BLOCK -> {
                    Block needed = placer.pendingBlockRequest;
                    if (needed != null) {
                        System.out.println("[GOAP] Need block: " + needed + " – queuing MineGoal");
                        bot.pushGoal(new MineGoal(needed));
                    } else {
                        System.out.println("[GOAP] PlaceGoal: no block type available.");
                        return GoalStatus.FAILED;
                    }
                }
                case DONE    -> { return GoalStatus.DONE; }
                case BLOCKED -> { return GoalStatus.FAILED; }
                case PLACING -> { /* continue */ }
            }
            return GoalStatus.RUNNING;
        }

        @Override public String toString() { return "PlaceGoal(" + pos + ", " + block + ")"; }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PickupGoal
    // ══════════════════════════════════════════════════════════════════════════

    class PickupGoal implements Goal {
        private final Vec3 origin;
        private int ticks = 0;

        PickupGoal(Vec3 origin) { this.origin = origin; }

        @Override
        public GoalStatus tick(CharlieBot bot) {
            if (mc.player == null || mc.level == null) return GoalStatus.FAILED;

            List<ItemEntity> drops = mc.level.getEntitiesOfClass(
                    ItemEntity.class,
                    new AABB(origin.subtract(PICKUP_SCAN_RADIUS, PICKUP_SCAN_RADIUS, PICKUP_SCAN_RADIUS),
                            origin.add   (PICKUP_SCAN_RADIUS, PICKUP_SCAN_RADIUS, PICKUP_SCAN_RADIUS)),
                    i -> !i.getItem().isEmpty());

            if (drops.isEmpty() || ticks++ > PICKUP_TIMEOUT) {
                pathfinder.stop(false);
                return GoalStatus.DONE;
            }

            ItemEntity closest = drops.stream()
                    .min(Comparator.comparingDouble(i -> mc.player.distanceToSqr(i)))
                    .orElse(null);

            if (closest != null) {
                RenderUtil.renderWireFrame(closest.blockPosition(), COLOR_PICKUP);

                if (mc.player.distanceTo(closest) > PICKUP_COLLECT_DIST) {
                    BlockPos dropPos = closest.blockPosition();
                    if (!pathfinder.isActive()) bot.pushGoal(new GoToGoal(dropPos));
                    else                        pathfinder.tick();
                } else {
                    pathfinder.stop(false);
                }
            }

            return GoalStatus.RUNNING;
        }

        @Override public String toString() { return "PickupGoal(" + origin + ")"; }
    }
}