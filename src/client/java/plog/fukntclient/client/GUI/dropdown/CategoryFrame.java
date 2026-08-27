package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.ColorRGBA;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Managers.GuiStateManager;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class CategoryFrame extends Frame {
    public Category cat;
    public boolean extended;
    public final List<ModuleFrame> modules = new ArrayList<>();

    private boolean dragging = false;
    private int dragX, dragY;
    private int scrollOffset = 0;

    public static final int MAX_VISIBLE_HEIGHT = 180;
    private static final int SCROLL_SPEED = 10;
    private static final float ANIM_DURATION_MS = 100f;

    private float expandProgress;
    private long lastNanoTime = System.nanoTime();

    private final GuiStateManager.CategoryState state;

    public CategoryFrame(Category cat, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(RenderUtil.pack(30, 30, 33, 255)),
                new ColorRGBA(RenderUtil.pack(40, 40, 44, 255)),
                new ColorRGBA(RenderUtil.pack(25, 25, 60, 255)),
                new ColorRGBA(RenderUtil.pack(255, 255, 255, 255)),
                cat.name());
        this.cat = cat;
        this.radius = 8;

        this.state = GuiStateManager.getCategory(cat.name());
        if (state.hasPosition) {
            this.x = state.x;
            this.y = state.y;
        }
        this.extended = state.extended;

        for (Module m : FukntClientClient.mm.getModules(cat)) {
            modules.add(new ModuleFrame(m, cat.name(), x, y, width, height));
        }
        for (int i = 0; i < modules.size(); i++) {
            modules.get(i).setLastInCategory(i == modules.size() - 1);
        }

        this.expandProgress = extended ? 1f : 0f;
    }

    private int getTotalContentHeight() {
        int h = 0;
        for (ModuleFrame mf : modules) h += mf.getTotalHeight();
        return h;
    }

    private float advanceAnim() {
        long now = System.nanoTime();
        float deltaMs = Math.min(50f, (now - lastNanoTime) / 1_000_000f);
        lastNanoTime = now;

        float target = extended ? 1f : 0f;
        float step = deltaMs / ANIM_DURATION_MS;
        if (expandProgress < target)      expandProgress = Math.min(target, expandProgress + step);
        else if (expandProgress > target) expandProgress = Math.max(target, expandProgress - step);
        return expandProgress;
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

        // Header is a standalone rounded pill when collapsed (or empty); flat-bottomed card top when expanded with content
        boolean headerRoundBottom = !extended || modules.isEmpty();
        RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius, extended ? enabledColor : mainColor,
                true, true, headerRoundBottom, headerRoundBottom);

        RenderUtil.renderText(graphics,
                x + (width / 2) - (FukntClientClient.mc.font.width(text) / 2),
                y + (height / 2) - 4,
                text, textColor, true);

        float progress = advanceAnim();
        if (progress <= 0.001f) return;

        int contentHeight  = getTotalContentHeight();
        int visibleHeight  = Math.min(contentHeight, MAX_VISIBLE_HEIGHT);
        int maxScroll      = Math.max(0, contentHeight - MAX_VISIBLE_HEIGHT);
        scrollOffset        = Math.max(0, Math.min(scrollOffset, maxScroll));

        int animatedVisibleHeight = Math.round(visibleHeight * RenderUtil.easeOutCubic(progress));
        if (animatedVisibleHeight <= 0) return;

        int clipTop    = y + height;
        int clipBottom = clipTop + animatedVisibleHeight;
        int screenH    = FukntClientClient.mc.getWindow().getGuiScaledHeight();

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

        if (progress > 0.99f && contentHeight > MAX_VISIBLE_HEIGHT) {
            int sbX = x + width - 2;
            int sbH = Math.max(12, (int) ((visibleHeight / (float) contentHeight) * visibleHeight));
            int sbY = clipTop + (int) ((scrollOffset / (float) maxScroll) * (visibleHeight - sbH));

            RenderUtil.renderRoundedBox2D(graphics, sbX, clipTop, 2, visibleHeight, 1,
                    new ColorRGBA(RenderUtil.pack(45, 45, 50, 255)));
            RenderUtil.renderRoundedBox2D(graphics, sbX, sbY, 2, sbH, 1,
                    new ColorRGBA(RenderUtil.pack(160, 160, 180, 255)));
        }
    }

    public void onScroll(double mouseX, double mouseY, double delta) {
        if (!extended) return;
        int visibleHeight = Math.min(getTotalContentHeight(), MAX_VISIBLE_HEIGHT);
        if (mouseX < x || mouseX > x + width) return;
        if (mouseY < y + height || mouseY > y + height + visibleHeight) return;

        scrollOffset -= (int) (delta * SCROLL_SPEED);
        int maxScroll = Math.max(0, getTotalContentHeight() - MAX_VISIBLE_HEIGHT);
        scrollOffset  = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (extended) {
            int clipBottom = y + height + Math.min(getTotalContentHeight(), MAX_VISIBLE_HEIGHT);
            for (ModuleFrame mf : modules) {
                if (mouseY >= y + height && mouseY <= clipBottom) {
                    mf.onClick(mouseX, mouseY, button);
                }
            }
        }

        if (!super.isMouseOver(mouseX, mouseY)) return;

        if (button == 0) {
            dragging = true;
            dragX = (int) mouseX - x;
            dragY = (int) mouseY - y;
        } else if (button == 1) {
            extended = !extended;
            scrollOffset = 0;
            state.extended = extended;
            GuiStateManager.save();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            state.x = this.x;
            state.y = this.y;
            state.hasPosition = true;
            GuiStateManager.save();
        }
        if (extended) {
            for (ModuleFrame mf : modules) mf.onRelease(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) return true;
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
            for (ModuleFrame mf : modules) mf.keyPressed(event);
        }
        return super.keyPressed(event);
    }

    public void onKey(int keyCode, char typedChar) {
        if (extended) {
            for (ModuleFrame mf : modules) mf.onKey(keyCode, typedChar);
        }
    }
}