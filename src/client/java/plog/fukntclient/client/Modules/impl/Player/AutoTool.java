package plog.fukntclient.client.Modules.impl.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;

@ModuleInfo(name = "AutoTool", description = "Switches to the best tool automatically", category = Category.Player)
public class AutoTool extends Module {

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        // Check if the player is currently mining a block
        if (mc.player == null || !mc.options.keyAttack.isDown() || mc.hitResult == null) return;

        // Ensure we are looking at a block
        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            BlockState state = mc.level.getBlockState(blockHit.getBlockPos());
            if (state.isAir()) return;

            int bestSlot = -1;
            float maxSpeed = 1.0f;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                float speed = stack.getDestroySpeed(state);
                if (speed > maxSpeed) {
                    maxSpeed = speed;
                    bestSlot = i;
                }
            }

            if (bestSlot != -1) {
                mc.player.getInventory().setSelectedSlot(bestSlot);
            }
        }
    }
}