package plog.fukntclient.client.Modules.impl.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "ChestStealer", description = "Auto-loots opened chests", category = Category.Player)
public class ChestStealer extends Module {

    public final DoubleSetting  delay       = new DoubleSetting("Delay (ticks)", 1.0, 0.0, 10.0, 1.0);
    public final BooleanSetting closeWhenDone = new BooleanSetting("Close When Done", true);
    public final BooleanSetting ignoreFullInv = new BooleanSetting("Ignore Full Inv", true);

    private int tickTimer = 0;

    public ChestStealer() {
        addSettings(delay, closeWhenDone, ignoreFullInv);
    }

    @Override
    public void onEnable() {
        tickTimer = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.gameMode == null) return;

        AbstractContainerMenu menu = mc.player.containerMenu;

        // Only act when a chest (or any ChestMenu) is open
        if (!(menu instanceof ChestMenu chestMenu)) return;

        if (ignoreFullInv.isActive() && isInventoryFull(mc)) return;

        tickTimer++;
        if (tickTimer < (int) delay.getVal()) return;
        tickTimer = 0;

        // Chest slots run from 0 up to the container size (excluding player inv)
        int chestSize = chestMenu.getContainer().getContainerSize();
        boolean tookItem = false;

        for (int slot = 0; slot < chestSize; slot++) {
            ItemStack stack = chestMenu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;

            // Shift-click to move the item straight to the player inventory
            mc.gameMode.handleInventoryMouseClick(
                    menu.containerId,
                    slot,
                    0,
                    ClickType.QUICK_MOVE,
                    mc.player
            );
            tookItem = true;
            break; // one item per tick to respect the delay setting
        }

        // Close the container once all slots are empty
        if (!tookItem && closeWhenDone.isActive()) {
            mc.player.closeContainer();
        }
    }

    private boolean isInventoryFull(Minecraft mc) {
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || s.getCount() < s.getMaxStackSize()) return false;
        }
        return true;
    }
}