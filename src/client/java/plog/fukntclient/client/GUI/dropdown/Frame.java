package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ColorRGBA;
import org.jetbrains.annotations.NotNull;
import plog.fukntclient.client.Util.RenderUtil;

public class Frame extends AbstractWidget {
    public int x, y;
    public int width, height;
    public int radius = 6; // bigger + smoother corner math in RenderUtil makes this read as rounded, not clipped

    public ColorRGBA mainColor;
    public ColorRGBA hoverColor;
    public ColorRGBA enabledColor;
    public ColorRGBA textColor;

    public boolean enabled = false;
    public boolean hovered = false;
    protected float hoverAnim = 0f;

    public String text;
    public int textAreaStart = 0;
    protected static final float TEXT_SCALE = 0.8f;

    public Frame(int x, int y, int width, int height,
                 ColorRGBA mainColor, ColorRGBA hoverColor,
                 ColorRGBA enabledColor, ColorRGBA textColor,
                 String text) {
        super(x, y, width, height, Component.empty());

        this.x            = x;
        this.y            = y;
        this.width        = width;
        this.height       = height;
        this.mainColor    = mainColor;
        this.hoverColor   = hoverColor;
        this.enabledColor = enabledColor;
        this.textColor    = textColor;
        this.text         = text;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        hoverAnim += ((hovered ? 1f : 0f) - hoverAnim) * 0.35f;

        ColorRGBA bg = enabled ? enabledColor : mainColor;
        RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius, bg);

        if (hoverAnim > 0.01f) {
            int alpha = (int) (hoverAnim * 40);
            RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius,
                    new ColorRGBA(RenderUtil.pack(255, 255, 255, alpha)));
        }

        if (enabled) {
            RenderUtil.renderRoundedBorder2D(graphics, x, y, width, height, radius,
                    new ColorRGBA(RenderUtil.pack(120, 170, 255, 200)));
        }

        int tStart = x + textAreaStart + 5;
        int tY = y + (height / 2) - 4;
        RenderUtil.renderTextScaled(graphics, (int) (tStart / TEXT_SCALE), (int) (tY / TEXT_SCALE),
                text, textColor, true, TEXT_SCALE);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        render(context, mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput builder) {
    }

    @Override
    public void onClick(MouseButtonEvent mouseButtonEvent, boolean bl) {
        onClick(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
    }

    @Override
    public void onRelease(MouseButtonEvent mouseButtonEvent) {
        onRelease(mouseButtonEvent.x(), mouseButtonEvent.y(), mouseButtonEvent.button());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(event.key(), event.scancode());
        char typedChar = (name != null && !name.isEmpty()) ? name.charAt(0) : (char) event.key();
        onKey(event.key(), typedChar);
        return true;
    }

    public void onClick(double mouseX, double mouseY, int button) {
        if (isMouseOver((int) mouseX, (int) mouseY)) {
            enabled = !enabled;
        }
    }

    public void onRelease(double mouseX, double mouseY, int button) {
    }

    public void onKey(int keyCode, char typedChar) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver((int) mouseX, (int) mouseY)) {
            enabled = !enabled;
        }
        return enabled;
    }

    @Override
    protected boolean isValidClickButton(@NotNull MouseButtonInfo button) {
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= (double) this.x &&
                mouseX <= (double) (this.x + this.width) &&
                mouseY >= (double) this.y &&
                mouseY <= (double) (this.y + this.height);
    }
}