package plog.fukntclient.client.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.Util.Rendering.CustomRenderPipeline;

public class RenderUtil {
    private static final Minecraft mc = Minecraft.getInstance();

    // ── World ─────────────────────────────────────────────────────────────────

    /**
     * Renders a wireframe (outline) box around the block position, visible through walls.
     */
    public static void renderWireFrame(BlockPos position, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        float x = position.getX(), y = position.getY(), z = position.getZ();
        pipeline.queueCornerBox(
                x, y, z, x + 1f, y + 1f, z + 1f,
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
        pipeline.queueTracerRibbon(
                (float) target.x, (float) target.y + 0.5f, (float) target.z,
                red(color), green(color), blue(color), alpha(color)
        );
    }

    public static void renderWireFrameAABB(AABB bb, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueCornerBox(
                (float) bb.minX, (float) bb.minY, (float) bb.minZ,
                (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ,
                red(color), green(color), blue(color), alpha(color)
        );
    }

    public static void renderFilledBoxAABB(AABB bb, ColorRGBA color) {
        CustomRenderPipeline pipeline = CustomRenderPipeline.getInstance();
        if (pipeline == null) return;
        pipeline.queueFilledBox(
                (float) bb.minX, (float) bb.minY, (float) bb.minZ,
                (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ,
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

    /** Continuous-circle corner fill — smoother stair-step than a naive Math.round approximation. */
    public static void renderRoundedBox2D(GuiGraphics graphics, int x, int y, int width, int height, int radius,
                                          ColorRGBA color, boolean roundTL, boolean roundTR, boolean roundBL, boolean roundBR) {
        radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (radius == 0 || !(roundTL || roundTR || roundBL || roundBR)) {
            renderBox2D(graphics, x, y, width, height, color);
            return;
        }

        renderBox2D(graphics, x + radius, y, width - radius * 2, height, color);
        renderBox2D(graphics, x, y + radius, radius, height - radius * 2, color);
        renderBox2D(graphics, x + width - radius, y + radius, radius, height - radius * 2, color);

        double rF = radius - 0.5;
        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            double inside = rF * rF - dy * dy;
            int dxCurve = inside > 0 ? (int) Math.floor(radius - Math.sqrt(inside)) : radius;
            int wCurve = radius - dxCurve;

            if (roundTL) { if (wCurve > 0) renderBox2D(graphics, x + dxCurve, y + i, wCurve, 1, color); }
            else renderBox2D(graphics, x, y + i, radius, 1, color);

            if (roundTR) { if (wCurve > 0) renderBox2D(graphics, x + width - radius, y + i, wCurve, 1, color); }
            else renderBox2D(graphics, x + width - radius, y + i, radius, 1, color);

            if (roundBL) { if (wCurve > 0) renderBox2D(graphics, x + dxCurve, y + height - 1 - i, wCurve, 1, color); }
            else renderBox2D(graphics, x, y + height - 1 - i, radius, 1, color);

            if (roundBR) { if (wCurve > 0) renderBox2D(graphics, x + width - radius, y + height - 1 - i, wCurve, 1, color); }
            else renderBox2D(graphics, x + width - radius, y + height - 1 - i, radius, 1, color);
        }
    }

    public static void renderRoundedBox2D(GuiGraphics graphics, int x, int y, int width, int height, int radius, ColorRGBA color) {
        renderRoundedBox2D(graphics, x, y, width, height, radius, color, true, true, true, true);
    }

    public static void renderRoundedBorder2D(GuiGraphics graphics, int x, int y, int width, int height, int radius, ColorRGBA color) {
        renderBox2D(graphics, x, y, width, 1, color);
        renderBox2D(graphics, x, y + height - 1, width, 1, color);
        renderBox2D(graphics, x, y, 1, height, color);
        renderBox2D(graphics, x + width - 1, y, 1, height, color);
    }

    public static float easeOutCubic(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** Renders text at a fractional scale via a pose-stack transform (Minecraft's font has no native size param). */
    public static void renderTextScaled(GuiGraphics graphics, int x, int y, String text, ColorRGBA color, boolean shadow, float scale) {
        renderText(graphics, 0, 0, text, color, shadow);
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