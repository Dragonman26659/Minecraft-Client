package plog.fukntclient.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import plog.fukntclient.client.settings.Setting;

@Getter
public class DoubleSetting extends Setting {
    @Setter
    private double val, maxVal, minVal, increment, defaultVal;

    public DoubleSetting(String name, double defaultVal, double minVal, double maxVal, double increment) {
        this.name = name;
        this.defaultVal = defaultVal;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.increment = increment;
        this.val = defaultVal;
    }

    private double Clamp(double val, double min, double max) {
        return Math.min(max, Math.max(min, val));
    }
}
