package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.impl.ColorSetting;

@ModuleInfo(name = "ChestESP", description = "Highlights nearby chests", category = Category.Visual)
public class ChestESP extends Module {

    public final ColorSetting chestColor   = new ColorSetting("Chest Color",   255, 160,   0, 200);
    public final ColorSetting eChestColor  = new ColorSetting("E-Chest Color", 200,   0, 255, 200);
    public final ColorSetting shulkerColor = new ColorSetting("Shulker Color", 255,  50, 150, 200);
    public final ColorSetting barrelColor  = new ColorSetting("Barrel Color",  139,  90,  43, 200);

    public ChestESP() {
        addSettings(chestColor, eChestColor, shulkerColor, barrelColor);
    }

    @Override
    public void onRender() {
        if (FukntClientClient.mc.level == null || FukntClientClient.mc.player == null) return;

        int renderDistance = 6;
        int playerChunkX = FukntClientClient.mc.player.chunkPosition().x;
        int playerChunkZ = FukntClientClient.mc.player.chunkPosition().z;

        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                var chunk = FukntClientClient.mc.level.getChunk(playerChunkX + x, playerChunkZ + z);

                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();

                    if (entity instanceof ChestBlockEntity || entity instanceof TrappedChestBlockEntity) {
                        RenderUtil.renderFilledBox(pos, chestColor.getColorRGBA());
                    } else if (entity instanceof EnderChestBlockEntity) {
                        RenderUtil.renderFilledBox(pos, eChestColor.getColorRGBA());
                    } else if (entity instanceof ShulkerBoxBlockEntity) {
                        RenderUtil.renderFilledBox(pos, shulkerColor.getColorRGBA());
                    } else if (entity instanceof BarrelBlockEntity) {
                        RenderUtil.renderFilledBox(pos, barrelColor.getColorRGBA());
                    }
                }
            }
        }
    }
}