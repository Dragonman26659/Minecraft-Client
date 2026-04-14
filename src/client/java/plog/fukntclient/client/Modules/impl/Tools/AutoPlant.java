package plog.fukntclient.client.Modules.impl.Tools;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "AutoPlant", description = "Automatically plants seeds on tilled dirt", category = Category.Player)
public class AutoPlant extends Module {

    public final DoubleSetting range      = new DoubleSetting("Range", 4.0, 1.0, 6.0, 0.1);
    public final BooleanSetting equipSeeds = new BooleanSetting("Auto Equip Seeds", true);

    public AutoPlant() {
        addSettings(range, equipSeeds);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        int seedSlot = getSeedSlot(mc);
        if (seedSlot == -1) return;

        // If not already holding seeds, either equip them or bail
        ItemStack held = mc.player.getMainHandItem();
        boolean holdingSeeds = !held.isEmpty() && held.getItem() instanceof BlockItem bi &&
                (bi.getBlock() == Blocks.WHEAT || bi.getBlock() == Blocks.CARROTS ||
                        bi.getBlock() == Blocks.POTATOES || bi.getBlock() == Blocks.BEETROOTS);

        if (!holdingSeeds) {
            if (equipSeeds.isActive) {
                mc.player.getInventory().setSelectedSlot(seedSlot);
            } else {
                return;
            }
        }

        BlockPos playerPos = mc.player.blockPosition();
        double r = range.getVal();

        for (int x = (int) -r; x <= r; x++) {
            for (int z = (int) -r; z <= r; z++) {
                for (int y = (int) -r; y <= r; y++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (mc.level.getBlockState(pos).is(Blocks.FARMLAND) && mc.level.getBlockState(pos.above()).isAir()) {
                        int oldSlot = mc.player.getInventory().getSelectedSlot();
                        mc.player.getInventory().setSelectedSlot(seedSlot);

                        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX(), pos.getY(), pos.getZ()), Direction.UP, pos, false);
                        assert mc.gameMode != null;
                        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                        mc.player.swing(InteractionHand.MAIN_HAND);

                        mc.player.getInventory().setSelectedSlot(oldSlot);
                        return;
                    }
                }
            }
        }
    }

    private int getSeedSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            assert mc.player != null;
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.WHEAT || bi.getBlock() == Blocks.CARROTS ||
                        bi.getBlock() == Blocks.POTATOES || bi.getBlock() == Blocks.BEETROOTS) {
                    return i;
                }
            }
        }
        return -1;
    }
}