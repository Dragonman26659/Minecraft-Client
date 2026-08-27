package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ColorRGBA;
import org.lwjgl.glfw.GLFW;
import plog.fukntclient.client.Util.RenderUtil;
import plog.fukntclient.client.settings.Setting;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ColorSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.KeySetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

public class SettingFrame extends Frame {
    public Setting setting;
    public ModuleFrame parent;

    private boolean listeningForKey = false;
    private boolean slidingSlider   = false;
    private boolean colorPickerOpen = false;
    private int activeChannel = -1;
    private boolean roundBottom = false;

    private static final int PICKER_ROW_HEIGHT = 10;
    private static final int PICKER_ROWS       = 5;
    private static final float ANIM_DURATION_MS = 100f;

    private float pickerProgress = 0f;
    private long lastNanoTime = System.nanoTime();

    public SettingFrame(Setting setting, ModuleFrame parent, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(RenderUtil.pack(75, 75, 80, 255)),
                new ColorRGBA(RenderUtil.pack(90, 90, 95, 255)),
                new ColorRGBA(RenderUtil.pack(75, 75, 80, 255)),
                new ColorRGBA(RenderUtil.pack(230, 230, 230, 255)),
                setting.getName());
        this.setting = setting;
        this.parent  = parent;
        this.radius  = 8; // matches Category/ModuleFrame radius for a consistent bottom-of-card curve
    }

    /** Called by ModuleFrame each render — true only when this is the very last setting in the last module. */
    public void setRoundBottom(boolean b) {
        this.roundBottom = b;
    }

    public int getTotalHeight() {
        if (setting instanceof ColorSetting && (colorPickerOpen || pickerProgress > 0.001f)) {
            int fullPickerHeight = PICKER_ROW_HEIGHT * PICKER_ROWS;
            return height + Math.round(fullPickerHeight * RenderUtil.easeOutCubic(pickerProgress));
        }
        return height;
    }

    private float advanceAnim() {
        long now = System.nanoTime();
        float deltaMs = Math.min(50f, (now - lastNanoTime) / 1_000_000f);
        lastNanoTime = now;

        float target = colorPickerOpen ? 1f : 0f;
        float step = deltaMs / ANIM_DURATION_MS;
        if (pickerProgress < target)      pickerProgress = Math.min(target, pickerProgress + step);
        else if (pickerProgress > target) pickerProgress = Math.max(target, pickerProgress - step);
        return pickerProgress;
    }

    private boolean isLeftHeld() {
        long win = GLFW.glfwGetCurrentContext();
        return GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    private boolean inBounds(double mouseX, double mouseY, int rowY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= rowY && mouseY < rowY + PICKER_ROW_HEIGHT;
    }

    private void updateSlider(double mouseX) {
        if (setting instanceof DoubleSetting ds) {
            double pct = Math.min(1, Math.max(0, (mouseX - x) / (double) width));
            ds.setVal(ds.getMinVal() + pct * (ds.getMaxVal() - ds.getMinVal()));
        }
    }

    private void updateColorChannel(ColorSetting cs, int ch, double mouseX) {
        double pct = Math.min(1, Math.max(0, (mouseX - x) / (double) width));
        int val = (int) (pct * 255);
        switch (ch) {
            case 0 -> cs.setR(val);
            case 1 -> cs.setG(val);
            case 2 -> cs.setB(val);
            case 3 -> cs.setA(val);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);
        boolean mousePressed = isLeftHeld();

        if (!mousePressed) {
            slidingSlider = false;
            activeChannel = -1;
        }

        // Sharp on all corners except the bottom, and only when this is the true last piece of the card
        RenderUtil.renderRoundedBox2D(graphics, x, y, width, height, radius, hovered ? hoverColor : mainColor,
                false, false, roundBottom, roundBottom);

        if (setting instanceof DoubleSetting ds) {
            if (slidingSlider) updateSlider(mouseX);

            double pct = (ds.getVal() - ds.getMinVal()) / (ds.getMaxVal() - ds.getMinVal());
            int fillWidth = Math.max(4, (int) (width * pct));
            RenderUtil.renderRoundedBox2D(graphics, x, y, fillWidth, height, radius,
                    new ColorRGBA(RenderUtil.pack(90, 140, 220, 255)), false, false, roundBottom, false);

            int knobSize = height;
            int knobX = Math.min(x + width - knobSize, Math.max(x, x + fillWidth - knobSize / 2));
            RenderUtil.renderRoundedBox2D(graphics, knobX, y, knobSize, knobSize, knobSize / 2,
                    new ColorRGBA(RenderUtil.pack(255, 255, 255, 255)));

        } else if (setting instanceof ColorSetting cs) {
            float progress = advanceAnim();

            if (progress > 0.001f) {
                if (activeChannel >= 0 && activeChannel < 4) {
                    updateColorChannel(cs, activeChannel, mouseX);
                }

                String[] labels = {"R", "G", "B", "A"};
                int[] values = {cs.getR(), cs.getG(), cs.getB(), cs.getA()};
                int[] barColors = {
                        RenderUtil.pack(200, 70, 70, 255),
                        RenderUtil.pack(70, 200, 70, 255),
                        RenderUtil.pack(70, 100, 220, 255),
                        RenderUtil.pack(170, 170, 170, 255)
                };

                int fullPickerHeight = PICKER_ROW_HEIGHT * PICKER_ROWS;
                int animatedHeight = Math.round(fullPickerHeight * RenderUtil.easeOutCubic(progress));
                int screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
                graphics.enableScissor(x, y + height, x + width, Math.min(y + height + animatedHeight, screenH));

                for (int i = 0; i < 4; i++) {
                    int rowY = y + height + (i * PICKER_ROW_HEIGHT);
                    double valPct = values[i] / 255.0;
                    RenderUtil.renderBox2D(graphics, x, rowY, width, PICKER_ROW_HEIGHT - 1,
                            new ColorRGBA(RenderUtil.pack(24, 24, 28, 255)));
                    RenderUtil.renderBox2D(graphics, x, rowY, Math.max(4, (int) (width * valPct)), PICKER_ROW_HEIGHT - 1,
                            new ColorRGBA(barColors[i]));
                    RenderUtil.renderText(graphics, x + 4, rowY + 1, labels[i] + ": " + values[i], textColor, true);
                }

                int rainbowY = y + height + (4 * PICKER_ROW_HEIGHT);
                boolean pickerRoundBottom = roundBottom; // the rainbow row is the true last row when picker is open
                int rainbowBg = cs.isRainbow() ? RenderUtil.pack(120, 80, 200, 255) : RenderUtil.pack(24, 24, 28, 255);
                RenderUtil.renderRoundedBox2D(graphics, x, rainbowY, width, PICKER_ROW_HEIGHT - 1, radius,
                        new ColorRGBA(rainbowBg), false, false, pickerRoundBottom, pickerRoundBottom);
                RenderUtil.renderText(graphics, x + 4, rainbowY + 1, "Rainbow: " + (cs.isRainbow() ? "ON" : "OFF"), textColor, true);

                graphics.disableScissor();
            }

            RenderUtil.renderRoundedBox2D(graphics, x + width - height + 2, y + 2, height - 4, height - 4, 3, cs.getColorRGBA());
        }

        String displayName = text;
        ColorRGBA displayColor = textColor;

        if (setting instanceof BooleanSetting bs) {
            displayColor = bs.isActive()
                    ? new ColorRGBA(RenderUtil.pack(100, 220, 120, 255))
                    : new ColorRGBA(RenderUtil.pack(170, 90, 90, 255));

        } else if (setting instanceof DoubleSetting ds) {
            displayName += ": " + String.format("%.2f", ds.getVal());

        } else if (setting instanceof ModeSetting ms) {
            displayName += ": " + ms.getCurrentMode();

        } else if (setting instanceof KeySetting ks) {
            if (listeningForKey) {
                displayName += ": ...";
                displayColor = new ColorRGBA(RenderUtil.pack(220, 90, 90, 255));
            } else {
                displayName += ": " + ks.getKeyChar();
            }

        } else if (setting instanceof ColorSetting) {
            displayName += colorPickerOpen ? " ▲" : " ▼";
        }

        RenderUtil.renderText(graphics, x + 6, y + (height / 2) - 4, displayName, displayColor, true);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (setting instanceof ColorSetting cs && (colorPickerOpen || pickerProgress > 0.5f)) {
            for (int i = 0; i < 4; i++) {
                int rowY = y + height + (i * PICKER_ROW_HEIGHT);
                if (inBounds(mouseX, mouseY, rowY)) {
                    if (button == 0) {
                        activeChannel = i;
                        updateColorChannel(cs, i, mouseX);
                    }
                    return;
                }
            }

            int rainbowY = y + height + (4 * PICKER_ROW_HEIGHT);
            if (inBounds(mouseX, mouseY, rainbowY)) {
                if (button == 0) cs.setRainbow(!cs.isRainbow());
                return;
            }
        }

        if (!isMouseOver(mouseX, mouseY)) return;

        if (button == 0) {
            if (setting instanceof DoubleSetting) {
                slidingSlider = true;
                updateSlider(mouseX);
            } else if (setting instanceof BooleanSetting bs) {
                bs.Toggle();
            } else if (setting instanceof ModeSetting ms) {
                ms.cycleForwards();
            } else if (setting instanceof KeySetting) {
                listeningForKey = !listeningForKey;
            }
        } else if (button == 1) {
            if (setting instanceof ModeSetting ms) {
                ms.cycleBackwards();
            } else if (setting instanceof ColorSetting) {
                colorPickerOpen = !colorPickerOpen;
                activeChannel = -1;
            }
        }
    }

    @Override
    public void onKey(int keyCode, char typedChar) {
        if (listeningForKey && setting instanceof KeySetting ks) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                ks.setKey(GLFW.GLFW_KEY_UNKNOWN);
                ks.setKeyChar('*');
            } else {
                ks.setKey(keyCode);
                ks.setKeyChar(typedChar);
            }
            listeningForKey = false;
        }
    }
}