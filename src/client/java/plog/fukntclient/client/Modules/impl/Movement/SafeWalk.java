package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;

@ModuleInfo(name = "SafeWalk", description = "Prevents walking off edges", category = Category.Movement)
public class SafeWalk extends Module {
    public SafeWalk() {}

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.onGround()) return;

        // Step slightly ahead in each movement axis and check for ground
        double x = mc.player.getDeltaMovement().x;
        double z = mc.player.getDeltaMovement().z;

        if (x != 0) {
            double nextX = mc.player.getX() + x;
            BlockPos below = BlockPos.containing(nextX, mc.player.getY() - 0.5, mc.player.getZ());
            if (mc.level.isEmptyBlock(below)) {
                // Cancel horizontal X movement to stop them walking off
                mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, mc.player.getDeltaMovement().z);
            }
        }

        if (z != 0) {
            double nextZ = mc.player.getZ() + z;
            BlockPos below = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, nextZ);
            if (mc.level.isEmptyBlock(below)) {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, mc.player.getDeltaMovement().y, 0);
            }
        }
    }

    @Override
    public void onDisable() {
        if (FukntClientClient.mc.player != null)
            FukntClientClient.mc.player.setShiftKeyDown(false);
    }
}