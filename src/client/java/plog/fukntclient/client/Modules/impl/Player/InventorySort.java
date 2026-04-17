package plog.fukntclient.client.Modules.impl.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleInfo(name = "InventorySort", description = "Sorts and stacks your inventory", category = Category.Player)
public class InventorySort extends Module {

    public final ModeSetting    mode       = new ModeSetting("Sort By", "Item ID", "Stack Size", "None");
    public final BooleanSetting stackFirst = new BooleanSetting("Stack First", true);
    public final BooleanSetting hotbar     = new BooleanSetting("Include Hotbar", false);

    private boolean sorted = false;

    public InventorySort() {
        addSettings(mode, stackFirst, hotbar);
    }

    @Override
    public void onEnable() {
        sorted = false;
    }

    @Override
    public void onDisable() {
        sorted = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;
        if (mc.player.containerMenu != mc.player.inventoryMenu) return; // only sort own inv
        if (sorted) return;

        Inventory inv = mc.player.getInventory();

        // Slot range: 9–35 is the main inventory; 0–8 is the hotbar
        int startSlot = hotbar.isActive() ? 0 : 9;
        int endSlot   = 35;

        // Collect all non-empty stacks with their current slot indices
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }

        // Stack identical items together first
        if (stackFirst.isActive()) {
            stacks = mergeStacks(stacks);
        }

        // Sort by chosen strategy
        Comparator<ItemStack> comparator = switch (mode.getCurrentMode()) {
            case "Stack Size" -> Comparator.comparingInt(ItemStack::getCount).reversed();
            case "Item ID"    -> Comparator.comparing(s -> s.getItem().toString());
            default           -> null;
        };
        if (comparator != null) stacks.sort(comparator);

        // Write the sorted layout back directly (client-side for single-player;
        // for servers, pair this with inventory click packets as needed)
        int idx = startSlot;
        for (ItemStack stack : stacks) {
            inv.setItem(idx++, stack);
        }
        // Clear remaining slots that now hold duplicated references
        while (idx <= endSlot) {
            inv.setItem(idx++, ItemStack.EMPTY);
        }

        sorted = true;
    }

    /** Merges stacks of the same item up to their max stack size. */
    private List<ItemStack> mergeStacks(List<ItemStack> input) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack incoming : input) {
            boolean added = false;
            for (ItemStack existing : merged) {
                if (ItemStack.isSameItemSameComponents(existing, incoming)
                        && existing.getCount() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getCount();
                    int take  = Math.min(space, incoming.getCount());
                    existing.grow(take);
                    incoming.shrink(take);
                    if (incoming.isEmpty()) { added = true; break; }
                }
            }
            if (!added && !incoming.isEmpty()) merged.add(incoming);
        }
        return merged;
    }
}