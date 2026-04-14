package plog.fukntclient.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import plog.fukntclient.client.settings.Setting;

import java.util.Arrays;
import java.util.List;

@Getter
public class ModeSetting extends Setting {
    private final List<String> modes;

    @Setter
    private int modeIndex;
    @Setter
    private String currentMode;

    public ModeSetting(String name, String... modes) {
        this.name = name;
        this.modes = Arrays.asList(modes);
        this.modeIndex = this.modes.indexOf(modes[0]);
        if (currentMode == null) currentMode = modes[0];
    }


    public void cycleForwards () {
        modeIndex++;
        if (modeIndex > modes.size() - 1) { modeIndex = 0; }
        currentMode = modes.get(modeIndex);
    }

    public void cycleBackwards () {
        modeIndex--;
        if (modeIndex < 0) { modeIndex = modes.size() - 1; }
        currentMode = modes.get(modeIndex);
    }
}
