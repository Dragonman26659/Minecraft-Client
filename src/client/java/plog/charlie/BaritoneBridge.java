package plog.charlie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Charlie's only adapter to Baritone. All movement, mining and block placement
 * are delegated to Baritone processes; no local player-input implementation is
 * kept in Charlie.
 *
 * Reflection is intentional: it lets the client show a useful startup error if
 * the optional Baritone API jar has not been added to libs/.
 */
public final class BaritoneBridge {
    private static Object baritone;
    private static boolean attempted;

    private BaritoneBridge() {}

    public static synchronized void initialize() {
        if (attempted) return;
        attempted = true;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object provider = api.getMethod("getProvider").invoke(null);
            Class<?> providerApi = Class.forName("baritone.api.IBaritoneProvider");
            baritone = providerApi.getMethod("getPrimaryBaritone").invoke(provider);
            configureRendering(api);
        } catch (ReflectiveOperationException | LinkageError e) {
            baritone = null;
            System.err.println("[Charlie] Baritone unavailable. Check the bundled Baritone Fabric dependency: " + e);
        }
    }

    private static void configureRendering(Class<?> api) {
        try {
            Object settings = api.getMethod("getSettings").invoke(null);
            setSetting(settings, "renderPath", true);
            setSetting(settings, "renderPathAsLine", true);
            setSetting(settings, "renderGoal", true);
            setSetting(settings, "renderSelectionBoxes", true);
            setSetting(settings, "chatControl", true);
        } catch (ReflectiveOperationException e) {
            System.err.println("[Charlie] Could not configure all Baritone render settings: " + e.getMessage());
        }
    }

    private static void setSetting(Object settings, String name, boolean value) throws ReflectiveOperationException {
        Field field = settings.getClass().getField(name);
        Object setting = field.get(settings);
        setting.getClass().getField("value").set(setting, value);
    }

    public static boolean isAvailable() {
        initialize();
        return baritone != null;
    }

    private static Object call(Object target, String method) throws ReflectiveOperationException {
        return target.getClass().getMethod(method).invoke(target);
    }

    private static Object process(String getter) throws ReflectiveOperationException {
        if (!isAvailable()) throw new IllegalStateException("Baritone is not loaded");
        return call(baritone, getter);
    }

    public static void goTo(BlockPos pos) {
        try {
            Object customGoal = process("getCustomGoalProcess");
            Class<?> goalType = Class.forName("baritone.api.pathing.goals.GoalNear");
            Object goal = goalType.getConstructor(BlockPos.class, int.class).newInstance(pos, 1);
            Class<?> goalInterface = Class.forName("baritone.api.pathing.goals.Goal");
            customGoal.getClass().getMethod("setGoalAndPath", goalInterface).invoke(customGoal, goal);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not start Baritone navigation", e);
        }
    }

    /** Start Baritone's own mine process for the requested block type. */
    public static void mine(Block block) {
        try {
            Object mineProcess = process("getMineProcess");
            Class<?> blockClass = Block.class;
            Object blocks = java.lang.reflect.Array.newInstance(blockClass, 1);
            java.lang.reflect.Array.set(blocks, 0, block);
            mineProcess.getClass().getMethod("mine", blocks.getClass()).invoke(mineProcess, blocks);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not start Baritone mining", e);
        }
    }

    /**
     * Ask Baritone's builder process to place a one-block schematic. Baritone
     * chooses the route, placement face, interaction timing and inventory slot.
     */
    public static void place(BlockPos pos, BlockState state) {
        try {
            Object builder = process("getBuilderProcess");
            Class<?> schematicType = Class.forName("baritone.api.schematic.FillSchematic");
            Object schematic = schematicType
                    .getConstructor(int.class, int.class, int.class, BlockState.class)
                    .newInstance(1, 1, 1, state);
            Class<?> schematicInterface = Class.forName("baritone.api.schematic.ISchematic");
            Method build = builder.getClass().getMethod("build", String.class, schematicInterface, Vec3i.class);
            build.invoke(builder, "Charlie single-block placement", schematic, pos);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not start Baritone block placement", e);
        }
    }

    public static boolean isPathing() { return isActive("getCustomGoalProcess"); }
    public static boolean isMining() { return isActive("getMineProcess"); }
    public static boolean isBuilding() { return isActive("getBuilderProcess"); }

    private static boolean isActive(String getter) {
        try {
            return Boolean.TRUE.equals(call(process(getter), "isActive"));
        } catch (ReflectiveOperationException | IllegalStateException e) {
            return false;
        }
    }

    public static void stop() {
        if (!isAvailable()) return;
        try {
            Object behavior = call(baritone, "getPathingBehavior");
            behavior.getClass().getMethod("cancelEverything").invoke(behavior);
        } catch (ReflectiveOperationException e) {
            System.err.println("[Charlie] Could not stop Baritone: " + e.getMessage());
        }
    }
}
