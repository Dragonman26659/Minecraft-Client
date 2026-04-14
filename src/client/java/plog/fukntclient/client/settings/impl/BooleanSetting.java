package plog.fukntclient.client.settings.impl;

import lombok.Setter;
import plog.fukntclient.client.settings.Setting;

@Setter
public class BooleanSetting extends Setting {
    private boolean state;

    public BooleanSetting(String name, boolean state){
        this.name = name;
        this.state = state;
    }

    public boolean isActive() {
        return state;
    }

    public void Toggle() {
        state = !state;
    }
}
