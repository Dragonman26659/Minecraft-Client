package plog.fukntclient.client.Modules.impl.Tools;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;

@ModuleInfo(name = "AutoFish", description = "Automatically fishes and re-casts the rod", category = Category.Player)
public class AutoFish extends Module {

    public final BooleanSetting autoEquip = new BooleanSetting("Auto Equip Rod", true);

    public AutoFish() {
        addSettings(autoEquip);
    }

    @Override
    public void onTick() {
        var mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Try to equip rod if not currently holding one
        if (!(mc.player.getMainHandItem().getItem() instanceof FishingRodItem)) {
            if (autoEquip.isActive) {
                int rodSlot = getRodSlot();
                if (rodSlot == -1) return; // No rod found
                mc.player.getInventory().setSelectedSlot(rodSlot);
            } else {
                return; // Not holding rod and auto-equip is off
            }
        }

        FishingHook hook = mc.player.fishing;

        if (hook != null) {
            // A hook is in the water — check if something is biting
            // The hook bobs and its y velocity goes negative when a fish bites
            if (hook.getDeltaMovement().y < -0.1) {
                // Reel in
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                // Re-cast on the next tick will happen naturally since hook becomes null
            }
        } else {
            // No hook in water — cast the rod
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    private int getRodSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = FukntClientClient.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof FishingRodItem) {
                return i;
            }
        }
        return -1;
    }
}