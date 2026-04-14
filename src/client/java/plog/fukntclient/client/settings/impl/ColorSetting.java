package plog.fukntclient.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.ColorRGBA;
import plog.fukntclient.client.settings.Setting;

import java.awt.*;

@Getter
public class ColorSetting extends Setting {

    private int r, g, b, a;

    @Setter
    private boolean rainbow;

    @Getter @Setter
    private float rainbowOffset = 0.0f;

    public ColorSetting(String name, int r, int g, int b, int a) {
        this.name = name;
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    // Constructor with offset:
    public ColorSetting(String name, int r, int g, int b, int a, float rainbowOffset) {
        this(name, r, g, b, a);
        this.rainbowOffset = rainbowOffset;
    }

    // Convenience constructor — full opacity by default
    public ColorSetting(String name, int r, int g, int b) {
        this(name, r, g, b, 255);
    }

    public void setR(int r) { this.r = clamp(r); }
    public void setG(int g) { this.g = clamp(g); }
    public void setB(int b) { this.b = clamp(b); }
    public void setA(int a) { this.a = clamp(a); }

    public ColorRGBA getColorRGBA() {
        if (this.rainbow) {
            // 1. Calculate a hue based on time (e.g., a 4-second cycle)
            // 2. Add the offset so different settings don't look identical
            float speed = 4000.0f;
            float hue = ((System.currentTimeMillis() + (long)(rainbowOffset * 1000)) % (int)speed) / speed;

            // Convert HSB to a standard RGB integer
            int rgb = Color.HSBtoRGB(hue, 0.8f, 1.0f);

            // Re-apply the alpha channel from your setting
            int alpha = (this.a << 24);
            int finalColor = (alpha | (rgb & 0xFFFFFF));

            return new ColorRGBA(finalColor);
        }

        // Default back to static color if rainbow is off
        return new ColorRGBA(toPackedARGB());
    }

    // ColorRGBA expects a packed int in ARGB format: 0xAARRGGBB
    public int toPackedARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void fromPackedARGB(int packed) {
        this.a = (packed >> 24) & 0xFF;
        this.r = (packed >> 16) & 0xFF;
        this.g = (packed >> 8)  & 0xFF;
        this.b =  packed        & 0xFF;
    }

    private int clamp(int val) {
        return Math.min(255, Math.max(0, val));
    }
}