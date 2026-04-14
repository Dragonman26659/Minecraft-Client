package plog.fukntclient.client.Modules;

import com.mojang.blaze3d.platform.Window;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.events.PacketEvent;
import plog.fukntclient.client.settings.Setting;
import plog.fukntclient.client.settings.impl.KeySetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Module {

    private boolean toggled;
    private final String name, description;
    private final Category category;

    private final List<Setting> sList = new ArrayList<>();

    private boolean keyWasDown = false;

    @Setter
    public KeySetting key = new KeySetting("Keybind");

    public Module()
    {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        Validate.notNull(info, "CONFUSED ANNOTATION EXEPTION");

        this.name = info.name();
        this.description = info.description();
        this.category = info.category();

        addSetting(key);
    }

    protected void addSetting(Setting setting) {
        sList.add(setting);
    }

    protected void addSettings(Setting ... settings) {
        sList.addAll(Arrays.asList(settings));
    }

    public void onEnable() {

    }

    public void onDisable() {

    }

    public void OnToggle() {

    }

    public void onPacketSend(PacketEvent.Send event) {}


    public void onPacketReceive(PacketEvent.Receive event) {}

    public void toggle() {
        OnToggle();
        if (toggled) {
            toggled = false;
            onDisable();
        }
        else {
            toggled = true;
            onEnable();
        }
    }


    public void onTick() {

    }

    public void onRender() {

    }

    public void checkKeybind() {
        if (key.getKey() == org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN) return;
        if (FukntClientClient.mc.screen != null) return;

        long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        boolean isDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, key.getKey()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

        if (isDown && !keyWasDown) {
            toggle();
        }
        keyWasDown = isDown;
    }

    public void setToggled(boolean toggle) {
        if (toggle != toggled) toggle();
    }
}
