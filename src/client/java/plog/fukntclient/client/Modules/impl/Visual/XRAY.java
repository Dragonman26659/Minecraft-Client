package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import net.minecraft.util.ColorRGBA;

@ModuleInfo(name = "XRAY", description = "Highlights valuable ores", category = Category.Visual)
public class XRAY extends Module {

    public final DoubleSetting range = new DoubleSetting("Range", 16.0, 4.0, 64.0, 2.0);

    public XRAY() {
        addSettings(range);
    }

    @Override
    public void onRender() {
        BlockPos pPos = FukntClientClient.mc.player.blockPosition();
        int r = (int) range.getVal();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = pPos.offset(x, y, z);
                    var block = FukntClientClient.mc.level.getBlockState(pos).getBlock();

                    if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)
                        RenderUtil.renderFilledBox(pos, new ColorRGBA(RenderUtil.pack(0, 255, 255, 100)));

                    else if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)
                        RenderUtil.renderFilledBox(pos, new ColorRGBA(RenderUtil.pack(0, 255, 0, 100)));

                    else if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)
                        RenderUtil.renderFilledBox(pos, new ColorRGBA(RenderUtil.pack(200, 200, 200, 100)));

                    else if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE)
                        RenderUtil.renderFilledBox(pos, new ColorRGBA(RenderUtil.pack(200, 100, 0, 100)));

                    else if (block == Blocks.ANCIENT_DEBRIS)
                        RenderUtil.renderFilledBox(pos, new ColorRGBA(RenderUtil.pack(10, 10, 10, 100)));
                }
            }
        }
    }
}