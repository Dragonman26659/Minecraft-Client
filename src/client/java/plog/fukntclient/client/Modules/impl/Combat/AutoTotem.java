package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;

@ModuleInfo(name = "AutoTotem", description = "Keeps totem in offhand", category = Category.Combat)
public class AutoTotem extends Module {

    public AutoTotem() {}

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;

        // Basic checks: don't run if dead, in a chest/container, or if we already have a totem
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;

        ItemStack offhand = mc.player.getOffhandItem();
        if (offhand.getItem() == Items.TOTEM_OF_UNDYING) return;

        // Ensure the cursor is empty before we start moving things
        // Otherwise, we might accidentally 'drop' what the player is currently holding
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;

        Inventory inv = mc.player.getInventory();

        // Find a totem in the player's inventory
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {

                // 1. Map the inventory index to the Container Slot ID
                // In InventoryMenu:
                // 0-8: Crafting/Armor | 9-35: Main Inv | 36-44: Hotbar | 45: Offhand
                int slotId = i;
                if (i < 9) {
                    slotId = i + 36; // Hotbar slots (0-8) are 36-44 in the container
                }

                // 2. Pick up the totem
                clickSlot(slotId);

                // 3. Click the offhand slot (45)
                // This puts the totem in the offhand and puts whatever was there (shield/food) on the cursor
                clickSlot(45);

                // 4. Click the original slot again
                // This puts the old offhand item back into the slot the totem came from
                clickSlot(slotId);

                break;
            }
        }
    }

    private void clickSlot(int id) {
        Minecraft mc = FukntClientClient.mc;
        mc.gameMode.handleInventoryMouseClick(
                mc.player.inventoryMenu.containerId,
                id,
                0,
                ClickType.PICKUP,
                mc.player
        );
    }
}