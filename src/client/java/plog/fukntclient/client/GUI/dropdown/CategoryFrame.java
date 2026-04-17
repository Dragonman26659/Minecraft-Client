package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.ColorRGBA;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class CategoryFrame extends Frame {
    public Category cat;
    public boolean extended = true;
    public final List<ModuleFrame> modules = new ArrayList<>();

    private boolean dragging = false;
    private int dragX, dragY;
    private int scrollOffset = 0;

    public static final int MAX_VISIBLE_HEIGHT = 180;
    private static final int SCROLL_SPEED = 10;

    public CategoryFrame(Category cat, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(RenderUtil.pack(30, 30, 33, 255)),
                new ColorRGBA(RenderUtil.pack(40, 40, 44, 255)),
                new ColorRGBA(RenderUtil.pack(25, 25, 60, 255)),
                new ColorRGBA(RenderUtil.pack(255, 255, 255, 255)),
                cat.name());
        this.cat = cat;

        for (Module m : FukntClientClient.mm.getModules(cat)) {
            modules.add(new ModuleFrame(m, x, y, width, height));
        }
    }

    private int getTotalContentHeight() {
        int h = 0;
        for (ModuleFrame mf : modules) h += mf.getTotalHeight();
        return h;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (dragging) {
            this.x = mouseX - dragX;
            this.y = mouseY - dragY;
            int sw = FukntClientClient.mc.getWindow().getGuiScaledWidth();
            int sh = FukntClientClient.mc.getWindow().getGuiScaledHeight();
            this.x = Math.max(0, Math.min(this.x, sw - this.width));
            this.y = Math.max(0, Math.min(this.y, sh - this.height));
        }

        // Header
        RenderUtil.renderBox2D(graphics, x, y, width, height, extended ? enabledColor : mainColor);
        RenderUtil.renderText(graphics,
                x + (width / 2) - (FukntClientClient.mc.font.width(text) / 2),
                y + (height / 2) - 4,
                text, textColor, true);

        if (!extended) return;

        int contentHeight = getTotalContentHeight();
        int visibleHeight = Math.min(contentHeight, MAX_VISIBLE_HEIGHT);
        int maxScroll     = Math.max(0, contentHeight - MAX_VISIBLE_HEIGHT);
        scrollOffset      = Math.max(0, Math.min(scrollOffset, maxScroll));

        int clipTop    = y + height;
        int clipBottom = clipTop + visibleHeight;
        int screenH    = FukntClientClient.mc.getWindow().getGuiScaledHeight();

        // Scissor to visible area — use screen coords
        graphics.enableScissor(x, clipTop, x + width, Math.min(clipBottom, screenH));

        int currentY = clipTop - scrollOffset;
        for (ModuleFrame mf : modules) {
            mf.x = this.x;
            mf.y = currentY;
            mf.setPosition(this.x, currentY);
            mf.render(graphics, mouseX, mouseY);
            currentY += mf.getTotalHeight();
        }

        graphics.disableScissor();

        // Scrollbar track + thumb
        if (contentHeight > MAX_VISIBLE_HEIGHT) {
            int sbX = x + width - 2;
            int sbH = Math.max(12, (int) ((visibleHeight / (float) contentHeight) * visibleHeight));
            int sbY = clipTop + (int) ((scrollOffset / (float) maxScroll) * (visibleHeight - sbH));

            RenderUtil.renderBox2D(graphics, sbX, clipTop, 2, visibleHeight,
                    new ColorRGBA(RenderUtil.pack(45, 45, 50, 255)));
            RenderUtil.renderBox2D(graphics, sbX, sbY, 2, sbH,
                    new ColorRGBA(RenderUtil.pack(160, 160, 180, 255)));
        }
    }

    public void onScroll(double mouseX, double mouseY, double delta) {
        if (!extended) return;
        int visibleHeight = Math.min(getTotalContentHeight(), MAX_VISIBLE_HEIGHT);
        // Check mouse is over the panel
        if (mouseX < x || mouseX > x + width) return;
        if (mouseY < y + height || mouseY > y + height + visibleHeight) return;

        scrollOffset -= (int) (delta * SCROLL_SPEED);
        int maxScroll = Math.max(0, getTotalContentHeight() - MAX_VISIBLE_HEIGHT);
        scrollOffset  = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        // Forward to modules — they handle their own bounds checks
        if (extended) {
            int clipBottom = y + height + Math.min(getTotalContentHeight(), MAX_VISIBLE_HEIGHT);
            for (ModuleFrame mf : modules) {
                // Only forward clicks that are within the visible scroll window
                if (mouseY >= y + height && mouseY <= clipBottom) {
                    mf.onClick(mouseX, mouseY, button);
                }
            }
        }

        // Header — drag or collapse
        if (!super.isMouseOver(mouseX, mouseY)) return;

        if (button == 0) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
        } else if (button == 1) {
            extended = !extended;
            scrollOffset = 0;
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY, int button) {
        dragging = false;
        if (extended) {
            for (ModuleFrame mf : modules) mf.onRelease(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        // Check header collision
        if (super.isMouseOver(mouseX, mouseY)) return true;

        // If extended, check the bounds of the entire open dropdown
        if (extended) {
            int visibleHeight = Math.min(getTotalContentHeight(), MAX_VISIBLE_HEIGHT);
            return mouseX >= x && mouseX <= x + width &&
                    mouseY >= y + height && mouseY <= y + height + visibleHeight;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (extended) {
            for (ModuleFrame mf : modules) {
                mf.keyPressed(event);
            }
        }
        return super.keyPressed(event);
    }

    public void onKey(int keyCode, char typedChar) {
        if (extended) {
            for (ModuleFrame mf : modules) mf.onKey(keyCode, typedChar);
        }
    }
}