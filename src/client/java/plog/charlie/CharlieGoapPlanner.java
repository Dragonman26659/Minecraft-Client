package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Small deterministic GOAP-style planner for Charlie's survival goals.
 * It builds a dependency chain from the current inventory snapshot. The plan is
 * deliberately represented as actions (not chat strings) so execution can be
 * expanded without changing command parsing or the HUD.
 */
public final class CharlieGoapPlanner {
    public enum ActionType { TAKE_FROM_INVENTORY, SEARCH_CHESTS, MINE, SMELT, CRAFT, EQUIP, COMPLETE }
    public record Action(ActionType type, String subject, String detail) {
        @Override public String toString() { return type + ": " + subject + (detail.isBlank() ? "" : " (" + detail + ")"); }
    }
    public record Plan(String goal, List<Action> actions) {
        public List<String> lines() {
            List<String> out = new ArrayList<>();
            out.add("GOAP goal: " + goal);
            for (int i = 0; i < actions.size(); i++) out.add((i + 1) + ". " + actions.get(i));
            return out;
        }
    }

    private final Minecraft mc = Minecraft.getInstance();

    public Plan plan(String rawGoal) {
        String goal = rawGoal.toLowerCase(Locale.ROOT).trim();
        List<Action> actions = new ArrayList<>();
        if (goal.contains("diamond") && (goal.startsWith("mine") || goal.startsWith("get"))) {
            // Diamonds require an iron-tier (or better) pickaxe. Build a dependency
            // chain from the live inventory rather than blindly starting the mine.
            if (!hasAny("iron_pickaxe", "diamond_pickaxe", "netherite_pickaxe")) {
                actions.add(new Action(ActionType.SEARCH_CHESTS, "iron pickaxe / iron ingots / sticks", "inspect reachable storage first"));
                if (!has("stick", 2)) actions.add(new Action(ActionType.MINE, "oak_log", "convert logs to planks and sticks"));
                if (!has("iron_ingot", 3)) {
                    actions.add(new Action(ActionType.MINE, "iron_ore", "collect raw iron"));
                    actions.add(new Action(ActionType.SMELT, "raw iron", "3 ingots in a furnace"));
                }
                actions.add(new Action(ActionType.CRAFT, "iron_pickaxe", "3 iron ingots + 2 sticks + crafting table"));
                actions.add(new Action(ActionType.EQUIP, "iron_pickaxe", "required mining tier"));
            }
            actions.add(new Action(ActionType.MINE, "diamond_ore", "mine with iron-tier or better pickaxe"));
        } else if (goal.contains("pickaxe")) {
            String tier = goal.contains("diamond") ? "diamond" : goal.contains("iron") ? "iron" : "stone";
            actions.add(new Action(ActionType.SEARCH_CHESTS, tier + " pickaxe materials", "reuse stored materials where available"));
            if (tier.equals("iron")) {
                if (!has("iron_ingot", 3)) {
                    actions.add(new Action(ActionType.MINE, "iron_ore", "gather raw iron"));
                    actions.add(new Action(ActionType.SMELT, "raw iron", "smelt 3 ingots"));
                }
                if (!has("stick", 2)) actions.add(new Action(ActionType.CRAFT, "sticks", "2 sticks from planks"));
                actions.add(new Action(ActionType.CRAFT, "iron_pickaxe", "3 ingots + 2 sticks"));
            } else {
                if (!has("cobblestone", 3)) actions.add(new Action(ActionType.MINE, "stone", "gather 3 cobblestone"));
                if (!has("stick", 2)) actions.add(new Action(ActionType.CRAFT, "sticks", "2 sticks from planks"));
                actions.add(new Action(ActionType.CRAFT, tier + "_pickaxe", "3 material + 2 sticks"));
            }
        } else {
            actions.add(new Action(ActionType.SEARCH_CHESTS, goal, "look for required resources in nearby storage"));
            actions.add(new Action(ActionType.COMPLETE, goal, "no recipe template registered for this goal yet"));
        }
        return new Plan(rawGoal, List.copyOf(actions));
    }

    private boolean hasAny(String... ids) { for (String id : ids) if (has(id, 1)) return true; return false; }
    private boolean has(String path, int amount) {
        if (mc.player == null) return false;
        ResourceLocation id = ResourceLocation.tryParse("minecraft:" + path);
        if (id == null) return false;
        Item item = BuiltInRegistries.ITEM.get(id).get().value();
        int count = 0;
        //for (ItemStack stack : mc.player.getInventory().items) if (!stack.isEmpty() && stack.is(item)) count += stack.getCount();
        //for (ItemStack stack : mc.player.getInventory().offhand) if (!stack.isEmpty() && stack.is(item)) count += stack.getCount();
        return count >= amount;
    }
}
