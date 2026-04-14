package plog.fukntclient.client.Util;

import net.minecraft.core.BlockPos;
import plog.fukntclient.FukntClient;
import plog.fukntclient.client.FukntClientClient;

public class BaritoneUtil {

    private static boolean available = false;
    private static Object baritone;
    private static Object goalProcess;
    private static Object pathingBehavior;

    static {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = apiClass.getMethod("getProvider").invoke(null);
            baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            goalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            available = true;
            FukntClientClient.LOGGER.info("Baritone found and linked");
        } catch (Exception e) {
            FukntClientClient.LOGGER.warn("Baritone not found, pathing disabled");
        }
    }

    public static boolean isAvailable() { return available; }

    public static void goTo(BlockPos pos) {
        if (!available) return;
        try {
            Class<?> goalBlock = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goal = goalBlock.getConstructor(BlockPos.class).newInstance(pos);
            goalProcess.getClass().getMethod("setGoalAndPath",
                    Class.forName("baritone.api.pathing.goals.Goal")).invoke(goalProcess, goal);
        } catch (Exception e) {
            FukntClientClient.LOGGER.error("Baritone goTo failed", e);
        }
    }

    public static void stop() {
        if (!available) return;
        try {
            pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
        } catch (Exception e) {
            FukntClientClient.LOGGER.error("Baritone stop failed", e);
        }
    }

    public static boolean isPathing() {
        if (!available) return false;
        try {
            return (boolean) pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
        } catch (Exception e) {
            return false;
        }
    }
}
