package plog.fukntclient.client.Modules.impl.Tools;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.DoubleSetting;

import java.util.List;

@ModuleInfo(name = "AutoEat", description = "Automatically eats food", category = Category.Player)
public class AutoEat extends Module {

    public final DoubleSetting threshold = new DoubleSetting("Min Hunger", 15, 1, 20, 1);

    // Best → worst. First match in the hotbar wins.
    private static final List<Item> FOOD_PRIORITY = List.of(
            Items.GOLDEN_CARROT,        // 6 hunger, 14.4 saturation — best saturation in game
            Items.COOKED_BEEF,          // steak:     8 hunger, 12.8 saturation
            Items.COOKED_PORKCHOP,      // porkchop:  8 hunger, 12.8 saturation
            Items.COOKED_MUTTON,        // 6 hunger,  9.6 saturation
            Items.COOKED_CHICKEN,       // 6 hunger,  7.2 saturation
            Items.COOKED_SALMON,        // 6 hunger,  9.6 saturation
            Items.COOKED_COD,           // 5 hunger,  6.0 saturation
            Items.BREAD,                // 5 hunger,  6.0 saturation
            Items.CARROT,               // 3 hunger,  3.6 saturation
            Items.APPLE                 // 4 hunger,  2.4 saturation — fallback
    );

    public AutoEat() {
        addSetting(threshold);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.gameMode == null) return;

        // Stop using if hunger is satisfied
        if (mc.player.getFoodData().getFoodLevel() > threshold.getVal()) {
            mc.options.keyUse.setDown(false);
            return;
        }

        Inventory inv = mc.player.getInventory();
        int bestSlot     = -1;
        int bestPriority = Integer.MAX_VALUE;

        // Scan hotbar for the highest-priority food available
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            int priority = FOOD_PRIORITY.indexOf(stack.getItem());
            if (priority == -1) continue; // not a whitelisted food

            if (priority < bestPriority) {
                bestPriority = priority;
                bestSlot = i;
            }
        }

        if (bestSlot == -1) {
            // No recognised food found — release use key
            mc.options.keyUse.setDown(false);
            return;
        }

        // Switch to that slot and hold use
        inv.setSelectedSlot(bestSlot);
        mc.options.keyUse.setDown(true);
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.options != null)
            mc.options.keyUse.setDown(false);
    }
}