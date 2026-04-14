package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.util.ColorRGBA;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.Render;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ColorSetting;

@ModuleInfo(name = "Tracers", description = "Draws lines to entities", category = Category.Visual)
public class Tracers extends Module {

    public final ColorSetting playerColor = new ColorSetting("Player Color", 255, 255, 255, 255);
    public final ColorSetting friendColor = new ColorSetting("Friend Color", 0, 255, 255, 255);
    public final ColorSetting animalColor = new ColorSetting("Animal Color", 0, 255, 0, 255);
    public final ColorSetting mobColor = new ColorSetting("Mob Color", 255, 0, 0, 255);

    public final BooleanSetting traceMobs = new BooleanSetting("Mobs", false);
    public final BooleanSetting traceAnimals = new BooleanSetting("Animals", false);

    public Tracers() {
        // Ensure all settings are added to the menu
        addSettings(playerColor, friendColor, animalColor, mobColor, traceMobs, traceAnimals);
    }

    @Override
    public void onRender() {
        if (FukntClientClient.mc.level == null || FukntClientClient.mc.player == null) return;

        // Iterate through all entities in the world
        for (Entity entity : FukntClientClient.mc.level.entitiesForRendering()) {
            if (entity == FukntClientClient.mc.player) continue;

            ColorRGBA color = null;
            boolean shouldRender = false;

            if (entity instanceof Player player) {
                boolean isFriend = FukntClientClient.fm.isFriend(player.getGameProfile().name());
                color = isFriend ? friendColor.getColorRGBA() : playerColor.getColorRGBA();
                shouldRender = true;
            }
            else if (traceMobs.isActive() && entity instanceof Monster) {
                color = mobColor.getColorRGBA();
                shouldRender = true;
            }
            else if (traceAnimals.isActive() && entity instanceof Animal) {
                color = animalColor.getColorRGBA();
                shouldRender = true;
            }

            // If color was assigned (meaning the entity passed our filters), render the line
            if (shouldRender && color != null) {
                // Using entity.getEyePosition() often looks better for tracers than .position()
                Render.renderTracer(entity.position(), color);
            }
        }
    }
}