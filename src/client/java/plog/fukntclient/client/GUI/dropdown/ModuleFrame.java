package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ColorRGBA;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Managers.GuiStateManager;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.Setting;

import java.util.ArrayList;
import java.util.List;

public class ModuleFrame extends Frame {
    public Module mod;
    public boolean extended;
    public final List<SettingFrame> settings = new ArrayList<>();

    private static final float ANIM_DURATION_MS = 100f;
    private float expandProgress;
    private long lastNanoTime = System.nanoTime();

    private final GuiStateManager.ModuleState state;
    private boolean lastInCategory = false;

    public ModuleFrame(Module mod, String categoryName, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(RenderUtil.pack(35, 35, 40, 255)),
                new ColorRGBA(RenderUtil.pack(45, 45, 50, 255)),
                new ColorRGBA(RenderUtil.pack(35, 35, 40, 255)),
                new ColorRGBA(RenderUtil.pack(255, 255, 255, 255)),
                mod.getName());
        this.mod = mod;
        this.radius = 8; // matches CategoryFrame's radius so bottom-of-card curve is visually consistent

        this.state = GuiStateManager.getModule(categoryName, mod.getName());
        this.extended = state.extended;
        this.expandProgress = extended ? 1f : 0f;

        for (Setting s : mod.getSList()) {
            settings.add(new SettingFrame(s, this, x, y, width, height));
        }
    }

    /** Called once by CategoryFrame after building its module list. */
    public void setLastInCategory(boolean b) {
        this.lastInCategory = b;
    }

    private int totalSettingsHeight() {
        int h = 0;
        for (SettingFrame sf : settings) h += sf.getTotalHeight();
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
        hovered = isMouseOver(mouseX, mouseY);
        hoverAnim += ((hovered ? 1f : 0f) - hoverAnim) * 0.35f;

        // Bottom of the whole card is rounded only if this is the last module AND nothing is showing below it
        // (either it's collapsed, or it has no settings to expand into)
        boolean roundBottom = lastInCategory && (!extended || settings.isEmpty());

        RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius, mainColor,
                false, false, roundBottom, roundBottom);
        if (hoverAnim > 0.01f) {
            RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius,
                    new ColorRGBA(RenderUtil.pack(255, 255, 255, (int) (hoverAnim * 35))),
                    false, false, roundBottom, roundBottom);
        }

        ColorRGBA nameColor = mod.isToggled()
                ? new ColorRGBA(RenderUtil.pack(80, 220, 80, 255))
                : textColor;
        RenderUtil.renderText(graphics, x + 4, y + (height / 2) - 4, mod.getName(), nameColor, true);

        // Forward "last piece of the card" status to whichever setting will end up drawn last
        if (!settings.isEmpty()) {
            for (int i = 0; i < settings.size(); i++) {
                settings.get(i).setRoundBottom(lastInCategory && i == settings.size() - 1);
            }
        }

        float progress = advanceAnim();
        if (progress <= 0.001f) return;

        int fullHeight = totalSettingsHeight();
        int animatedHeight = Math.round(fullHeight * RenderUtil.easeOutCubic(progress));
        if (animatedHeight <= 0) return;

        int screenH = FukntClientClient.mc.getWindow().getGuiScaledHeight();
        graphics.enableScissor(x, y + height, x + width, Math.min(y + height + animatedHeight, screenH));

        int sOffset = height;
        for (SettingFrame sf : settings) {
            sf.x = this.x;
            sf.y = this.y + sOffset;
            sf.setPosition(this.x, this.y + sOffset);
            sf.setWidth(this.width);
            sf.render(graphics, mouseX, mouseY);
            sOffset += sf.getTotalHeight();
        }

        graphics.disableScissor();
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (extended) {
            for (SettingFrame sf : settings) {
                if (mouseX >= sf.x && mouseX <= sf.x + sf.width && mouseY >= sf.y && mouseY <= sf.y + sf.getTotalHeight()) {
                    sf.onClick(mouseX, mouseY, button);
                }
            }
        }

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (button == 0) {
                mod.toggle();
            } else if (button == 1) {
                extended = !extended;
                state.extended = extended;
                GuiStateManager.save();
            }
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY, int button) {
        for (SettingFrame sf : settings) sf.onRelease(mouseX, mouseY, button);
    }

    @Override
    public void onKey(int keyCode, char typedChar) {
        if (extended) {
            for (SettingFrame sf : settings) sf.onKey(keyCode, typedChar);
        }
    }

    public int getTotalHeight() {
        int h = height;
        if (expandProgress > 0.001f) {
            h += Math.round(totalSettingsHeight() * RenderUtil.easeOutCubic(expandProgress));
        }
        return h;
    }
}