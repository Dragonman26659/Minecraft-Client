package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ColorRGBA;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Util.Render;
import plog.fukntclient.client.settings.Setting;

import java.util.ArrayList;
import java.util.List;

public class ModuleFrame extends Frame {
    public Module mod;
    public boolean extended = false;
    public final List<SettingFrame> settings = new ArrayList<>();

    public ModuleFrame(Module mod, int x, int y, int width, int height) {
        super(x, y, width, height,
                new ColorRGBA(Render.pack(35, 35, 40, 255)),
                new ColorRGBA(Render.pack(45, 45, 50, 255)),
                new ColorRGBA(Render.pack(35, 35, 40, 255)),
                new ColorRGBA(Render.pack(255, 255, 255, 255)),
                mod.getName());
        this.mod = mod;

        for (Setting s : mod.getSList()) {
            settings.add(new SettingFrame(s, this, x, y, width, height));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        hovered = isMouseOver(mouseX, mouseY);

        Render.renderBox2D(graphics, x, y, width, height, hovered ? hoverColor : mainColor);

        ColorRGBA nameColor = mod.isToggled()
                ? new ColorRGBA(Render.pack(80, 220, 80, 255))
                : textColor;
        Render.renderText(graphics, x + 4, y + (height / 2) - 4, mod.getName(), nameColor, true);

        if (extended) {
            int sOffset = height;
            for (SettingFrame sf : settings) {
                sf.x = this.x;
                sf.y = this.y + sOffset;
                sf.setPosition(this.x, this.y + sOffset);
                sf.setWidth(this.width);
                sf.render(graphics, mouseX, mouseY);
                sOffset += sf.getTotalHeight();
            }
        }
    }

    // ModuleFrame.java
    @Override
    public void onClick(double mouseX, double mouseY, int button) {

        // 1. If the module is extended, check if we clicked a setting first
        if (extended) {
            for (SettingFrame sf : settings) {
                // Check if the mouse is within the setting's current x, y, width, and expanded height
                if (mouseX >= sf.x && mouseX <= sf.x + sf.width && mouseY >= sf.y && mouseY <= sf.y + sf.getTotalHeight()) {
                    sf.onClick(mouseX, mouseY, button);
                }
            }
        }

        // 2. If no setting was clicked, check if we clicked the module header itself
        // We only check the base 'height' here so clicking a setting doesn't toggle the mod
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (button == 0) {
                mod.toggle();
            } else if (button == 1) {
                extended = !extended;
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
            for (SettingFrame sf : settings)
                sf.onKey(keyCode, typedChar);
        }
    }

    public int getTotalHeight() {
        int h = height;
        if (extended) {
            for (SettingFrame sf : settings) h += sf.getTotalHeight();
        }
        return h;
    }
}