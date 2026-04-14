package plog.fukntclient.client.Modules.impl.Tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;

@ModuleInfo(name = "AutoBackPlace", description = "Automatically fills the tunnel behind you", category = Category.Player)
public class AutoBackPlace extends Module {

    private final BooleanSetting onlyWhileMining = new BooleanSetting("Only While Mining", true);
    private BlockPos lastPos = null;

    public AutoBackPlace() {
        addSettings(onlyWhileMining);
    }

    @Override
    public void onTick() {
        var mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        // Get the block position where the player's "trunk" just was
        BlockPos currentPos = mc.player.blockPosition();

        // If we moved to a new block
        if (lastPos != null && !lastPos.equals(currentPos)) {
            // Check if we should only place if mining (detecting left-click)
            if (onlyWhileMining.isActive() && !mc.options.keyAttack.isDown()) {
                lastPos = currentPos;
                return;
            }

            // Check if the previous position is now air and needs filling
            if (mc.level.getBlockState(lastPos).isAir()) {
                placeBlockAt(lastPos);
            }
        }

        lastPos = currentPos;
    }

    private void placeBlockAt(BlockPos pos) {
        var mc = FukntClientClient.mc;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        int blockSlot = findBlockInHotbar();

        if (blockSlot == -1) return; // No blocks to place

        // Switch slot, perform placement, switch back
        mc.player.getInventory().setSelectedSlot(blockSlot);

        // Create a fake hit result to "click" the block space
        Vec3 hitVec = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);

        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);

        mc.player.getInventory().setSelectedSlot(oldSlot);
    }

    private int findBlockInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = FukntClientClient.mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }
}