package plog.fukntclient.client.settings.impl;

import lombok.Getter;
import lombok.Setter;
import plog.fukntclient.client.settings.Setting;

@Getter
@Setter
public class KeySetting extends Setting {

    int key = org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;
    char keyChar = '*';

    public KeySetting(String name, int defaultKey) {
        this.name = name;
        this.key = defaultKey;
    }

    public KeySetting(String name) {
        this.name = name;
    }
}
