package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ColorSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "HUD", description = "Displays info on screen", category = Category.Visual)
public class Hud extends Module {

    public final BooleanSetting showModules      = new BooleanSetting("Show Modules", true);
    public final BooleanSetting showCoords       = new BooleanSetting("Show Coords", true);
    public final BooleanSetting showNetherCoords = new BooleanSetting("Nether Coords", true);
    public final BooleanSetting showBiome        = new BooleanSetting("Show Biome", true);
    public final BooleanSetting showTarget = new BooleanSetting("Show Target", true);
    public final BooleanSetting showFps          = new BooleanSetting("Show FPS", true);
    public final BooleanSetting showDirection    = new BooleanSetting("Show Direction", true);
    public final BooleanSetting showHealth       = new BooleanSetting("Show Health", true);
    public final BooleanSetting background       = new BooleanSetting("Background", true);

    public final ColorSetting textColor   = new ColorSetting("Text Color",   255, 255, 255, 255);
    public final ColorSetting accentColor = new ColorSetting("Accent Color", 180,  80, 255, 255);
    public final ColorSetting bgColor     = new ColorSetting("BG Color",       0,   0,   0, 120);

    private static final int LINE_HEIGHT = 10;
    private static final int PADDING     = 2;

    public Hud() {
        addSettings(showModules, showCoords, showNetherCoords, showBiome, showFps,
                showDirection, showTarget, showHealth, background, textColor, accentColor, bgColor);
    }

    @Override
    public void onRender() {
        // HUD rendering is handled via a GuiGraphics mixin — see renderHud()
    }

    public void renderHud(GuiGraphics graphics) {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        int screenW = mc.getWindow().getGuiScaledWidth();

        List<String[]> lines = buildLines(mc);

        int hudWidth  = getMaxWidth(mc, lines) + PADDING * 2;
        int hudHeight = lines.size() * LINE_HEIGHT + PADDING * 2;

        int startX = screenW - hudWidth - 2;
        int startY = 2;

        if (background.isActive()) {
            RenderUtil.renderBox2D(graphics,
                    startX - 1, startY - 1,
                    hudWidth + 2, hudHeight + 2,
                    bgColor.getColorRGBA());
        }

        int y = startY + PADDING;
        for (String[] line : lines) {
            String label = line[0];
            String value = line[1];
            int x = startX + PADDING;

            if (accentColor.isRainbow()) {
                renderRainbowText(graphics, x, y, label, true);
            } else {
                RenderUtil.renderText(graphics, x, y, label, accentColor.getColorRGBA(), true);
            }

            int labelWidth = mc.font.width(label);

            if (textColor.isRainbow()) {
                renderRainbowText(graphics, x + labelWidth, y, value, true);
            } else {
                RenderUtil.renderText(graphics, x + labelWidth, y, value, textColor.getColorRGBA(), true);
            }

            y += LINE_HEIGHT;
        }

        if (showModules.isActive()) {
            renderModuleList(graphics, mc, startX, startY + hudHeight);
        }
    }

    private List<String[]> buildLines(Minecraft mc) {
        List<String[]> lines = new ArrayList<>();

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        if (showCoords.isActive()) {
            lines.add(new String[]{"XYZ: ", String.format("%.1f / %.1f / %.1f", x, y, z)});
        }

        if (showNetherCoords.isActive()) {
            ResourceKey<Level> dim = mc.level.dimension();
            if (dim == Level.OVERWORLD) {
                lines.add(new String[]{"Nether: ", String.format("%.1f / %.1f / %.1f", x / 8, y, z / 8)});
            } else if (dim == Level.NETHER) {
                lines.add(new String[]{"Overworld: ", String.format("%.1f / %.1f / %.1f", x * 8, y, z * 8)});
            }
        }

        if (showBiome.isActive()) {
            lines.add(new String[]{"Biome: ", getBiomeName(mc)});
        }

        if (showTarget.isActive() && FukntClientClient.tm.hasTarget()) {
            String currentDim = mc.level.dimension().location().toString();
            // Get the coordinates scaled correctly for the dimension we are currently in
            BlockPos scaledPos = FukntClientClient.tm.getScaledPos(currentDim);

            double dist = mc.player.distanceToSqr(scaledPos.getX(), scaledPos.getY(), scaledPos.getZ());
            String distStr = String.format("%.1fm", Math.sqrt(dist));

            // Calculate if looking at the scaled target
            String directionStatus = isLookingAt(mc, scaledPos) ? "§aLocked" : "§cAway";

            lines.add(new String[]{"Target: ", distStr + " (" + directionStatus + "§r)"});
        }

        if (showDirection.isActive()) {
            lines.add(new String[]{"Facing: ", getDirection(mc)});
        }

        if (showHealth.isActive()) {
            float health = mc.player.getHealth();
            float max    = mc.player.getMaxHealth();
            lines.add(new String[]{"Health: ", String.format("%.1f / %.1f", health, max)});
        }

        if (showFps.isActive()) {
            lines.add(new String[]{"FPS: ", String.valueOf(FukntClientClient.mc.getFps())});
        }

        return lines;
    }

    private String getBiomeName(Minecraft mc) {
        if (mc.level == null || mc.player == null) return "Unknown";

        Holder<Biome> biomeHolder = mc.level.getBiome(mc.player.blockPosition());

        // This attempts to get the registry name (e.g., minecraft:plains) and formats it
        return biomeHolder.unwrapKey()
                .map(key -> {
                    String path = key.location().getPath(); // "plains"
                    return path.substring(0, 1).toUpperCase() + path.substring(1).replace('_', ' ');
                })
                .orElse("Unknown");
    }

    private boolean isLookingAt(Minecraft mc, BlockPos pos) {
        double diffX = pos.getX() + 0.5 - mc.player.getX();
        double diffZ = pos.getZ() + 0.5 - mc.player.getZ();
        float targetYaw = (float) Math.toDegrees(Math.atan2(-diffX, diffZ));

        float playerYaw = mc.player.getYRot() % 360;
        if (playerYaw < 0) playerYaw += 360;
        if (targetYaw < 0) targetYaw += 360;

        float diff = Math.abs(playerYaw - targetYaw);
        // 15-degree tolerance window
        return diff < 15 || diff > 345;
    }

    private void renderModuleList(GuiGraphics graphics, Minecraft mc, int hudX, int listStartY) {
        List<Module> active = FukntClientClient.mm.getModules().values().stream()
                .filter(m -> m.isToggled() && !(m instanceof Hud))
                .sorted((a, b) -> mc.font.width(b.getName()) - mc.font.width(a.getName()))
                .toList();

        if (active.isEmpty()) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int y = listStartY + 2;

        for (Module mod : active) {
            String name  = mod.getName();
            int textW    = mc.font.width(name);
            int x        = screenW - textW - PADDING - 2;

            if (background.isActive()) {
                RenderUtil.renderBox2D(graphics,
                        x - PADDING, y - 1,
                        textW + PADDING * 2, LINE_HEIGHT + 1,
                        bgColor.getColorRGBA());
            }

            if (accentColor.isRainbow()) {
                renderRainbowText(graphics, x, y, name, true);
            } else {
                RenderUtil.renderText(graphics, x, y, name, accentColor.getColorRGBA(), true);
            }
            y += LINE_HEIGHT;
        }
    }

    private int getMaxWidth(Minecraft mc, List<String[]> lines) {
        int max = 60;
        for (String[] line : lines) {
            int w = mc.font.width(line[0] + line[1]);
            if (w > max) max = w;
        }
        return max;
    }

    private String getDirection(Minecraft mc) {
        float yaw = mc.player.getYRot() % 360;
        if (yaw < 0) yaw += 360;

        if (yaw >= 315 || yaw < 45) return "South (+Z)";
        if (yaw < 135)              return "West (-X)";
        if (yaw < 225)              return "North (-Z)";
        return                             "East (+X)";
    }

    private void renderRainbowText(GuiGraphics graphics, int x, int y, String text, boolean shadow) {
        Minecraft mc = FukntClientClient.mc;
        float speed = 4000.0f;
        float offsetPerChar = 0.05f; // hue shift between each character

        int currentX = x;
        for (int i = 0; i < text.length(); i++) {
            float hue = ((System.currentTimeMillis() + (long)(i * offsetPerChar * 1000)) % (int)speed) / speed;
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
            // Pack as ARGB with full alpha
            int color = (0xFF << 24) | (rgb & 0xFFFFFF);

            String ch = String.valueOf(text.charAt(i));
            graphics.drawString(mc.font, ch, currentX, y, color, shadow);
            currentX += mc.font.width(ch);
        }
    }
}