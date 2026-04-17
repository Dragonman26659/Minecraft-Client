package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ColorSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

@ModuleInfo(name = "ESP", description = "Highlights players, mobs and animals through walls", category = Category.Visual)
public class ESP extends Module {

    public final ModeSetting    renderMode    = new ModeSetting("Mode", "Outline", "Fill", "Both");

    public final BooleanSetting showPlayers   = new BooleanSetting("Players",  true);
    public final BooleanSetting showFriends   = new BooleanSetting("Friends",  true);
    public final BooleanSetting showEnemies   = new BooleanSetting("Enemies",  true);
    public final BooleanSetting showMobs      = new BooleanSetting("Mobs",     true);
    public final BooleanSetting showAnimals   = new BooleanSetting("Animals",  false);
    public final BooleanSetting ignoreSelf    = new BooleanSetting("Ignore Self", true);

    public final ColorSetting   playerColor   = new ColorSetting("Player Color",  255,  50,  50, 200);
    public final ColorSetting   friendColor   = new ColorSetting("Friend Color",   50, 255,  50, 200);
    public final ColorSetting   enemyColor    = new ColorSetting("Enemy Color",   255, 0, 100, 200);
    public final ColorSetting   mobColor      = new ColorSetting("Mob Color",     255, 165,   0, 200);
    public final ColorSetting   animalColor   = new ColorSetting("Animal Color",   50, 200, 255, 200);

    public final DoubleSetting  renderRange   = new DoubleSetting("Range", 64.0, 16.0, 256.0, 8.0);

    public ESP() {
        addSettings(renderMode, showPlayers, showFriends, showEnemies, showMobs, showAnimals,
                ignoreSelf, playerColor, enemyColor, friendColor, mobColor, animalColor, renderRange);
    }

    @Override
    public void onRender() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!entity.isAlive()) continue;
            if (ignoreSelf.isActive() && entity == mc.player) continue;
            if (mc.player.distanceTo(entity) > renderRange.getVal()) continue;

            ColorRGBA color = resolveColor(entity);
            if (color == null) continue;

            renderEntity(entity, color);
        }
    }

    // -------------------------------------------------------------------------

    private ColorRGBA resolveColor(Entity entity) {
        if (entity instanceof Player player) {
            String name = player.getGameProfile().name();
            if (FukntClientClient.fm.isFriend(name)) {
                return showFriends.isActive() ? friendColor.getColorRGBA() : null;
            }
            if (FukntClientClient.fm.isEnemy(name)) {
                return showEnemies.isActive() ? enemyColor.getColorRGBA() : null; // Check enemy toggle
            }
            return showPlayers.isActive() ? playerColor.getColorRGBA() : null;
        }
        if (entity instanceof Monster) return showMobs.isActive()    ? mobColor.getColorRGBA()    : null;
        if (entity instanceof Animal)  return showAnimals.isActive() ? animalColor.getColorRGBA() : null;
        return null;
    }

    private void renderEntity(Entity entity, ColorRGBA color) {
        switch (renderMode.getCurrentMode()) {
            case "Outline" -> renderOutline(entity, color);
            case "Fill"    -> renderFill(entity, color);
            case "Both"    -> { renderOutline(entity, color); renderFill(entity, withAlpha(color, 60)); }
        }
    }

    private void renderOutline(Entity entity, ColorRGBA color) {
        AABB bb = entity.getBoundingBox().inflate(0.05);
        RenderUtil.renderWireFrameAABB(bb, color);
    }

    private void renderFill(Entity entity, ColorRGBA color) {
        AABB bb = entity.getBoundingBox();
        RenderUtil.renderFilledBoxAABB(bb, color);
    }

    /** Returns a copy of the color with a different alpha (0-255). */
    private ColorRGBA withAlpha(ColorRGBA color, int alpha) {
        int rgba = color.rgba();
        int r = (rgba >> 16) & 0xFF;
        int g = (rgba >> 8)  & 0xFF;
        int b =  rgba        & 0xFF;
        return new ColorRGBA(RenderUtil.pack(r, g, b, alpha));
    }
}