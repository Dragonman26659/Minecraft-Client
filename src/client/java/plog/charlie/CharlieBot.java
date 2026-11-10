package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Charlie is the single owner of bot goals and Baritone processes.
 * Baritone performs every movement, mining and placement action. Charlie only
 * sequences high-level goals and exposes the current plan to the HUD.
 */
public class CharlieBot {
    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<Goal> goalStack = new ArrayDeque<>();
    private final CharlieGoapPlanner goapPlanner = new CharlieGoapPlanner();
    private final CharlieCombatController combat = new CharlieCombatController();
    private CharlieGoapPlanner.Plan lastPlan;

    public CharlieBot() {
        BaritoneBridge.initialize();
    }

    public void goTo(BlockPos pos) {
        clearGoals();
        pushGoal(new GoToGoal(pos));
    }

    public void mine(Block block) {
        clearGoals();
        pushGoal(new MineGoal(block));
    }

    public void place(BlockPos pos, Block block) {
        clearGoals();
        pushGoal(new PlaceGoal(pos, block == null ? null : block.defaultBlockState()));
    }

    public void placeAny(BlockPos pos) {
        clearGoals();
        pushGoal(new PlaceGoal(pos, null));
    }

    public void stop() {
        clearGoals();
        combat.stop();
        BaritoneBridge.stop();
    }

    public CharlieGoapPlanner.Plan plan(String goal) {
        lastPlan = goapPlanner.plan(goal);
        return lastPlan;
    }

    public CharlieGoapPlanner.Plan getLastPlan() { return lastPlan; }
    public void startCombat(String playerName) { clearGoals(); BaritoneBridge.stop(); combat.start(playerName); }
    public boolean isCombatActive() { return combat.isEnabled(); }

    /** Find loaded chest blocks around the player; does not force-load chunks. */
    public List<BlockPos> findNearbyChests(int radius) {
        List<BlockPos> found = new ArrayList<>();
        if (mc.player == null || mc.level == null) return found;
        BlockPos origin = mc.player.blockPosition();
        int r = Math.max(1, Math.min(radius, 32));
        for (int x = -r; x <= r; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    var state = mc.level.getBlockState(pos);
                    if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) found.add(pos.immutable());
                }
            }
        }
        found.sort(java.util.Comparator.comparingDouble(pos -> mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        return found;
    }

    public void onTick() {
        combat.tick();
        if (mc.player == null || mc.level == null || goalStack.isEmpty()) return;
        Goal current = goalStack.peek();
        GoalStatus status = current.tick();
        if (status == GoalStatus.DONE) {
            goalStack.pop();
        } else if (status == GoalStatus.FAILED) {
            System.err.println("[Charlie] Goal failed: " + current);
            goalStack.pop();
            BaritoneBridge.stop();
        }
    }

    /** Baritone renders its own route, goal, mining and builder overlays. */
    public void onRender() {
        // Intentionally empty: do not draw a second, competing path overlay.
    }

    public List<String> getPlanLines() {
        List<String> lines = new ArrayList<>();
        if (combat.isEnabled()) lines.add("Combat: fast tactical loop");
        if (lastPlan != null) lines.addAll(lastPlan.lines());
        for (Goal goal : goalStack) lines.add(goal.toString());
        return lines;
    }

    public String getCurrentPlan() {
        if (combat.isEnabled()) return "Combat: active";
        if (lastPlan != null) return "GOAP: " + lastPlan.goal();
        return goalStack.isEmpty() ? "Idle" : goalStack.peek().toString();
    }

    private void pushGoal(Goal goal) { goalStack.push(goal); }
    private void clearGoals() { goalStack.clear(); }

    private enum GoalStatus { RUNNING, DONE, FAILED }
    private interface Goal {
        GoalStatus tick();
    }

    private final class GoToGoal implements Goal {
        private final BlockPos target;
        private boolean started;
        private boolean sawActive;
        private int idleTicks;

        private GoToGoal(BlockPos target) { this.target = target.immutable(); }

        @Override public GoalStatus tick() {
            if (mc.player.blockPosition().closerThan(target, 2.0)) {
                if (started) BaritoneBridge.stop();
                return GoalStatus.DONE;
            }
            if (!started) {
                try {
                    BaritoneBridge.goTo(target);
                    started = true;
                } catch (IllegalStateException e) {
                    System.err.println("[Charlie] " + e.getMessage());
                    return GoalStatus.FAILED;
                }
                return GoalStatus.RUNNING;
            }
            if (BaritoneBridge.isPathing()) {
                sawActive = true;
                idleTicks = 0;
            } else if (sawActive) {
                // The goal process has finished. Re-check distance before deciding.
                return mc.player.blockPosition().closerThan(target, 2.0)
                        ? GoalStatus.DONE : GoalStatus.FAILED;
            } else if (++idleTicks > 80) {
                return GoalStatus.FAILED;
            }
            return GoalStatus.RUNNING;
        }

        @Override public String toString() {
            return "Baritone: navigate to " + target.getX() + ", " + target.getY() + ", " + target.getZ();
        }
    }

    private final class MineGoal implements Goal {
        private final Block block;
        private boolean started;
        private boolean sawActive;
        private int startupTicks;

        private MineGoal(Block block) { this.block = block; }

        @Override public GoalStatus tick() {
            if (!started) {
                try {
                    BaritoneBridge.mine(block);
                    started = true;
                } catch (IllegalStateException e) {
                    System.err.println("[Charlie] " + e.getMessage());
                    return GoalStatus.FAILED;
                }
                return GoalStatus.RUNNING;
            }
            if (BaritoneBridge.isMining()) {
                sawActive = true;
                startupTicks = 0;
                return GoalStatus.RUNNING;
            }
            if (sawActive) return GoalStatus.DONE;
            // Give Baritone time to take process control on its next client tick.
            return ++startupTicks > 80 ? GoalStatus.FAILED : GoalStatus.RUNNING;
        }

        @Override public String toString() { return "Baritone: mine " + block; }
    }

    private final class PlaceGoal implements Goal {
        private final BlockState requestedState;
        private boolean started;
        private boolean sawActive;
        private int startupTicks;
        private BlockPos target;

        private PlaceGoal(BlockPos target, BlockState requestedState) {
            this.target = target.immutable();
            this.requestedState = requestedState;
        }

        @Override public GoalStatus tick() {
            if (!started) {
                BlockState state = requestedState;
                if (state == null) state = findHotbarBlockState();
                if (state == null) {
                    System.err.println("[Charlie] Place goal needs a block in the hotbar.");
                    return GoalStatus.FAILED;
                }
                if (!mc.level.getBlockState(target).isAir()) return GoalStatus.DONE;
                try {
                    BaritoneBridge.place(target, state);
                    started = true;
                } catch (IllegalStateException e) {
                    System.err.println("[Charlie] " + e.getMessage());
                    return GoalStatus.FAILED;
                }
                return GoalStatus.RUNNING;
            }
            if (!mc.level.getBlockState(target).isAir()) return GoalStatus.DONE;
            if (BaritoneBridge.isBuilding()) {
                sawActive = true;
                startupTicks = 0;
                return GoalStatus.RUNNING;
            }
            if (sawActive) return GoalStatus.FAILED;
            return ++startupTicks > 80 ? GoalStatus.FAILED : GoalStatus.RUNNING;
        }

        private BlockState findHotbarBlockState() {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = mc.player.getInventory().getItem(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                    return blockItem.getBlock().defaultBlockState();
                }
            }
            return null;
        }

        @Override public String toString() { return "Baritone: place " + (requestedState == null ? "hotbar block" : requestedState.getBlock()) + " at " + target.getX() + ", " + target.getY() + ", " + target.getZ(); }
    }
}
