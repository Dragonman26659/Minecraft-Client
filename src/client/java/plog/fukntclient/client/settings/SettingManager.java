package plog.fukntclient.client.settings;

import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ColorSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;
import plog.fukntclient.client.Modules.Module;

import java.awt.*;
import java.util.ArrayList; // Added import
import java.util.List;

public class SettingManager {

    // FIX 1: Initialize the list immediately so it's never null
    public List<Setting> settingList = new ArrayList<>();

    public void onTick() {
        // FIX 2: Guard clause - if the list is somehow null or empty, stop here
        if (settingList == null || settingList.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Setting setting : settingList) {
            // FIX 3: Check if the individual setting in the list is null
            if (setting instanceof ColorSetting color && color.isRainbow()) {
                color.fromPackedARGB(getRainbowColor(now, color.getRainbowOffset()));
            }
        }
    }

    public BooleanSetting getBooleanSetting(Module mod, String settingName) {
        // FIX 4: Safety check for the Module object
        if (mod == null || mod.getSList() == null) return null;

        for (Setting setting : mod.getSList()) {
            if (setting instanceof BooleanSetting bs) {
                if (bs.getName().equalsIgnoreCase(settingName)) {
                    return bs;
                }
            }
        }
        return null;
    }

    public ModeSetting getModeSetting(Module mod, String settingName) {
        if (mod == null || mod.getSList() == null) return null;

        for (Setting setting : mod.getSList()) {
            if (setting instanceof ModeSetting ms) {
                if (ms.getName().equalsIgnoreCase(settingName)) {
                    return ms;
                }
            }
        }
        return null;
    }

    public DoubleSetting getDoubleSetting(Module mod, String settingName) {
        if (mod == null || mod.getSList() == null) return null;

        for (Setting setting : mod.getSList()) {
            if (setting instanceof DoubleSetting ds) {
                if (ds.getName().equalsIgnoreCase(settingName)) {
                    return ds;
                }
            }
        }
        return null;
    }

    public Setting getSetting(Module mod, String settingName) {
        if (mod == null || mod.getSList() == null) return null;

        for (Setting setting : mod.getSList()) {
            if (setting != null && setting.getName().equalsIgnoreCase(settingName)) {
                return setting;
            }
        }
        return null;
    }

    private int getRainbowColor(long time, float offset) {
        float hue = ((time / 2000.0f) + offset) % 1.0f;
        Color rgb = Color.getHSBColor(hue, 1.0f, 1.0f);
        return (0xFF << 24) | (rgb.getRed() << 16) | (rgb.getGreen() << 8) | rgb.getBlue();
    }
}