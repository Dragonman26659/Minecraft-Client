package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.impl.ColorSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "HoleESP", description = "Highlights safe holes for PvP", category = Category.Visual)
public class HoleESP extends Module {

    public final DoubleSetting range = new DoubleSetting("Range", 8.0, 1.0, 20.0, 1.0);
    public final ColorSetting safeColor = new ColorSetting("Bedrock Color", 0, 255, 0, 150);
    public final ColorSetting obiColor = new ColorSetting("Obsidian Color", 255, 0, 0, 150);

    public HoleESP() {
        addSettings(range, safeColor, obiColor);
    }

    @Override
    public void onRender() {
        if (FukntClientClient.mc.player == null || FukntClientClient.mc.level == null) return;

        BlockPos pPos = FukntClientClient.mc.player.blockPosition();
        int r = (int) range.getVal();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = pPos.offset(x, y, z);
                    checkAndRenderHole(pos);
                }
            }
        }
    }

    private void checkAndRenderHole(BlockPos pos) {
        // The hole itself must be air, and the space above it must also be air (to stand in)
        if (!FukntClientClient.mc.level.getBlockState(pos).isAir() ||
                !FukntClientClient.mc.level.getBlockState(pos.above()).isAir()) return;

        BlockPos[] neighbors = {
                pos.below(), pos.north(), pos.south(), pos.east(), pos.west()
        };

        int bedrockCount = 0;
        int obsidianCount = 0;

        for (BlockPos neighbor : neighbors) {
            var block = FukntClientClient.mc.level.getBlockState(neighbor).getBlock();
            if (block == Blocks.BEDROCK) {
                bedrockCount++;
            } else if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN || block == Blocks.RESPAWN_ANCHOR) {
                obsidianCount++;
            } else {
                return; // Not a safe hole if any neighbor is air/breakable
            }
        }

        // Render logic
        if (bedrockCount == 5) {
            RenderUtil.renderBaseFilled(pos, safeColor.getColorRGBA());
        } else if (bedrockCount + obsidianCount == 5) {
            RenderUtil.renderBaseFilled(pos, obiColor.getColorRGBA());
        }
    }
}