package plog.fukntclient.client.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.Rendering.CustomRenderPipeline;

public class Render {
    private static final Minecraft mc = Minecraft.getInstance();

    // ── World ─────────────────────────────────────────────────────────────────

    /**
     * Renders a wireframe (outline) box around the block position, visible through walls.
     */
    public static void renderWireFrame(BlockPos position, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueWireFrame(
                position.getX(), position.getY(), position.getZ(),
                position.getX() + 1f, position.getY() + 1f, position.getZ() + 1f,
                red(color), green(color), blue(color), alpha(color)
        );
    }

    /**
     * Renders a fully filled box covering the block position, visible through walls.
     */
    public static void renderFilledBox(BlockPos position, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueFilledBox(
                position.getX(), position.getY(), position.getZ(),
                position.getX() + 1f, position.getY() + 1f, position.getZ() + 1f,
                red(color), green(color), blue(color), alpha(color)
        );
    }

    /**
     * Renders only the bottom face of a block as a filled quad, visible through walls.
     */
    public static void renderBaseFilled(BlockPos position, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueBaseFilled(
                position.getX(), position.getY(), position.getZ(),
                red(color), green(color), blue(color), alpha(color)
        );
    }

    /**
     * Renders a line from the camera to the target world position, visible through walls.
     */
    public static void renderTracer(Vec3 target, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueTracer(
                (float) target.x, (float) target.y + 0.5f, (float) target.z,
                red(color), green(color), blue(color), alpha(color)
        );
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    /**
     * @param x      Top-left X screen coordinate
     * @param y      Top-left Y screen coordinate
     * @param width  Box width in pixels
     * @param height Box height in pixels
     * @param color  Fill color
     */
    public static void renderBox2D(GuiGraphics context, int x, int y, int width, int height, ColorRGBA color) {
        context.fill(x, y, x + width, y + height, color.rgba());
    }

    /**
     * @param x      Top-left X screen coordinate
     * @param y      Top-left Y screen coordinate
     * @param text   String to render
     * @param color  Text color
     * @param shadow If true, renders a drop shadow
     */
    public static void renderText(GuiGraphics context, int x, int y, String text, ColorRGBA color, boolean shadow) {
        context.drawString(mc.font, text, x, y, color.rgba(), shadow);
    }

    /**
     * Packs RGBA components into a single int (ARGB format).
     */
    public static int pack(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    // ── Color helpers ─────────────────────────────────────────────────────────

    private static float red(ColorRGBA c)   { return ((c.rgba() >> 16) & 0xFF) / 255f; }
    private static float green(ColorRGBA c) { return ((c.rgba() >> 8)  & 0xFF) / 255f; }
    private static float blue(ColorRGBA c)  { return ( c.rgba()        & 0xFF) / 255f; }
    private static float alpha(ColorRGBA c) { return ((c.rgba() >> 24) & 0xFF) / 255f; }
}