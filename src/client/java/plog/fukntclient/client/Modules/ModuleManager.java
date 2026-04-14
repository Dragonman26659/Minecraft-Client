package plog.fukntclient.client.Modules;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.reflections.Reflections;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.settings.impl.KeySetting;

import java.util.HashMap;
import java.util.Set;

@Getter
public class ModuleManager {

    private final HashMap<Class<? extends Module>, Module> modules;

    public ModuleManager() {
        this.modules = new HashMap<>();
        register();

        FukntClientClient.EVENT_BUS.onSend(event -> {
            for (Module mod : modules.values()) {
                if (mod.isToggled()) {
                    try {
                        mod.onPacketSend(event);
                    } catch (Exception e) {
                        FukntClientClient.LOGGER.error("Error in onPacketSend for module {}: ", mod.getName(), e);
                    }
                }
            }
        });

        FukntClientClient.EVENT_BUS.onReceive(event -> {
            for (Module mod : modules.values()) {
                if (mod.isToggled()) {
                    try {
                        mod.onPacketReceive(event);
                    } catch (Exception e) {
                        FukntClientClient.LOGGER.error("Error in onPacketReceive for module {}: ", mod.getName(), e);
                    }
                }
            }
        });
    }

    public final Module getModule(Class<? extends Module> mod) {
        return modules.get(mod);
    }

    public final Module getModule(String name) {
        for (Module mod : modules.values()) {
            if (mod.getName().equalsIgnoreCase(name)) {
                return mod;
            }
        }
        return null;
    }

    public final Module[] getModules(Category cat) {
        return getModules().values().stream()
                .filter(module -> module.getCategory() == cat)
                .toArray(Module[]::new);
    }

    public final void register() {
        final Reflections refl = new Reflections("plog.fukntclient.client.Modules.impl");
        final Set<Class<? extends Module>> classes = refl.getSubTypesOf(Module.class);

        for (Class<? extends Module> aClass : classes) {
            try {
                final Module feat = aClass.newInstance();
                modules.put(aClass, feat);
                FukntClientClient.LOGGER.info("{} loaded", aClass.getSimpleName());
            } catch (InstantiationException | IllegalAccessException e) {
                FukntClientClient.LOGGER.error("Failed to load module: {}", aClass.getSimpleName(), e);
            }
        }
    }

    public final void unregister(Module... module) {
        for (Module mod : module) {
            modules.remove(mod.getClass());
        }
    }

    public void onTick() {
        for (Module mod : modules.values()) {
            mod.checkKeybind();

            if (mod.isToggled()) {
                try {
                    mod.onTick();
                } catch (Exception e) {
                    FukntClientClient.LOGGER.error("Error in onTick for module {}: ", mod.getName(), e);
                }
            }
        }
    }

    public void onRender() {
        for (Module mod : modules.values()) {
            if (mod.isToggled()) {
                try {
                    mod.onRender();
                } catch (Exception e) {
                    FukntClientClient.LOGGER.error("Error in onRender for module {}: ", mod.getName(), e);
                }
            }
        }
    }
}