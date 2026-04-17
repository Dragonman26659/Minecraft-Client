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

    private static final int PICKER_ROW_HEIGHT = 10;
    private static final int PICKER_ROWS       = 5;

    public SettingFrame(Setting setting, ModuleFrame parent, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(RenderUtil.pack(75, 75, 80, 255)),
                new ColorRGBA(RenderUtil.pack(90, 90, 95, 255)),
                new ColorRGBA(RenderUtil.pack(75, 75, 80, 255)),
                new ColorRGBA(RenderUtil.pack(230, 230, 230, 255)),
                setting.getName());
        this.setting = setting;
        this.parent  = parent;
    }

    public int getTotalHeight() {
        if (setting instanceof ColorSetting && colorPickerOpen)
            return height + (PICKER_ROW_HEIGHT * PICKER_ROWS);
        return height;
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

        // Reset interaction states if the mouse is released
        if (!mousePressed) {
            slidingSlider = false;
            activeChannel = -1;
        }

        RenderUtil.renderBox2D(graphics, x, y, width, height, hovered ? hoverColor : mainColor);

        if (setting instanceof DoubleSetting ds) {
            // Continuous update if dragging
            if (slidingSlider) updateSlider(mouseX);

            // Render the progress fill for the slider
            double pct = (ds.getVal() - ds.getMinVal()) / (ds.getMaxVal() - ds.getMinVal());
            int fillWidth = (int) (width * pct);
            RenderUtil.renderBox2D(graphics, x, y, fillWidth, height, new ColorRGBA(RenderUtil.pack(60, 100, 160, 255)));

        } else if (setting instanceof ColorSetting cs && colorPickerOpen) {
            // Update the active RGB/A slider if currently dragging one
            if (activeChannel >= 0 && activeChannel < 4) {
                updateColorChannel(cs, activeChannel, mouseX);
            }

            // Define color picker visual data
            String[] labels = {"R", "G", "B", "A"};
            int[] values = {cs.getR(), cs.getG(), cs.getB(), cs.getA()};
            int[] barPackedColors = {
                    RenderUtil.pack(180, 50, 50, 255),  // Red
                    RenderUtil.pack(50, 180, 50, 255),  // Green
                    RenderUtil.pack(50, 80, 200, 255),  // Blue
                    RenderUtil.pack(160, 160, 160, 255) // Alpha
            };

            // Render the 4 RGBA Sliders
            for (int i = 0; i < 4; i++) {
                int rowY = y + height + (i * PICKER_ROW_HEIGHT);
                double valPct = values[i] / 255.0;

                // Row background
                RenderUtil.renderBox2D(graphics, x, rowY, width, PICKER_ROW_HEIGHT, new ColorRGBA(RenderUtil.pack(28, 28, 32, 255)));
                // Progress bar
                RenderUtil.renderBox2D(graphics, x, rowY, (int) (width * valPct), PICKER_ROW_HEIGHT, new ColorRGBA(barPackedColors[i]));
                // Label
                RenderUtil.renderText(graphics, x + 3, rowY + 1, labels[i] + ": " + values[i], textColor, true);
            }

            // Render the Rainbow Toggle Row
            int rainbowY = y + height + (4 * PICKER_ROW_HEIGHT);
            int rainbowBg = cs.isRainbow() ? RenderUtil.pack(90, 50, 150, 255) : RenderUtil.pack(28, 28, 32, 255);
            RenderUtil.renderBox2D(graphics, x, rainbowY, width, PICKER_ROW_HEIGHT, new ColorRGBA(rainbowBg));
            RenderUtil.renderText(graphics, x + 3, rainbowY + 1, "Rainbow: " + (cs.isRainbow() ? "ON" : "OFF"), textColor, true);

            // Render the preview swatch on the main setting row
            RenderUtil.renderBox2D(graphics, x + width - height, y, width, height, cs.getColorRGBA());
        }

        String displayName = text;
        ColorRGBA displayColor = textColor;

        if (setting instanceof BooleanSetting bs) {
            displayColor = bs.isActive()
                    ? new ColorRGBA(RenderUtil.pack(80, 220, 80, 255))
                    : new ColorRGBA(RenderUtil.pack(180, 60, 60, 255));

        } else if (setting instanceof DoubleSetting ds) {
            displayName += ": " + String.format("%.2f", ds.getVal());

        } else if (setting instanceof ModeSetting ms) {
            displayName += ": " + ms.getCurrentMode();

        } else if (setting instanceof KeySetting ks) {
            if (listeningForKey) {
                displayName += ": ...";
                displayColor = new ColorRGBA(RenderUtil.pack(220, 80, 80, 255));
            } else {
                displayName += ": " + ks.getKeyChar();
            }

        } else if (setting instanceof ColorSetting) {
            displayName += colorPickerOpen ? " ▲" : " ▼";
        }

        RenderUtil.renderText(graphics, x + 4, y + (height / 2) - 4, displayName, displayColor, true);
    }
    @Override
    public void onClick(double mouseX, double mouseY, int button) {

        // 1. Handle Color Picker (Sliders & Rainbow)
        // These are outside the main 'height', so we check them before the hover check.
        if (setting instanceof ColorSetting cs && colorPickerOpen) {
            for (int i = 0; i < 4; i++) {
                int rowY = y + height + (i * PICKER_ROW_HEIGHT);
                if (inBounds(mouseX, mouseY, rowY)) {
                    if (button == 0) {
                        activeChannel = i;
                        updateColorChannel(cs, i, mouseX);
                    }
                    return; // Stop processing once handled
                }
            }

            int rainbowY = y + height + (4 * PICKER_ROW_HEIGHT);
            if (inBounds(mouseX, mouseY, rainbowY)) {
                if (button == 0) cs.setRainbow(!cs.isRainbow());
            }
        }

        // 2. Handle the Main Setting Row
        // If the mouse isn't over this specific frame, ignore the click!
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