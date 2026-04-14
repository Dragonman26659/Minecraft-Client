package plog.fukntclient.client.config;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.settings.Setting;
import plog.fukntclient.client.settings.impl.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {

    // Initialize Gson with pretty printing for readable config files
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Directory and File instances
    private static final File DIR = new File(Minecraft.getInstance().gameDirectory, "fuknt");
    private static final File CONFIG_FILE = new File(DIR, "config.json");

    public static void saveConfig() {
        if (!DIR.exists()) {
            DIR.mkdirs();
        }

        JsonObject root = new JsonObject();
        JsonObject modulesObj = new JsonObject();

        for (Module module : FukntClientClient.mm.getModules().values()) {
            JsonObject modObj = new JsonObject();

            // Save module toggle state
            modObj.addProperty("toggled", module.isToggled());

            // Save individual settings
            JsonObject settingsObj = new JsonObject();
            for (Setting setting : module.getSList()) {
                if (setting instanceof BooleanSetting bs) {
                    settingsObj.addProperty(setting.getName(), bs.isActive());
                } else if (setting instanceof DoubleSetting ds) {
                    settingsObj.addProperty(setting.getName(), ds.getVal());
                } else if (setting instanceof ModeSetting ms) {
                    settingsObj.addProperty(setting.getName(), ms.getCurrentMode());
                } else if (setting instanceof KeySetting ks) {
                    settingsObj.addProperty(setting.getName(), ks.getKey());
                } else if (setting instanceof ColorSetting cs) {
                    JsonObject colorObj = new JsonObject();
                    colorObj.addProperty("packed", cs.toPackedARGB());
                    colorObj.addProperty("rainbow", cs.isRainbow());
                    settingsObj.add(setting.getName(), colorObj);
                }
            }
            modObj.add("Settings", settingsObj);
            modulesObj.add(module.getName(), modObj);
        }

        root.add("Modules", modulesObj);

        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(root, writer);
            FukntClientClient.LOGGER.info("Successfully saved config.");
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to save config!", e);
        }
    }

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) return;

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("Modules")) return;

            JsonObject modulesObj = root.getAsJsonObject("Modules");

            for (String modName : modulesObj.keySet()) {
                Module module = FukntClientClient.mm.getModule(modName);
                if (module == null) continue;

                JsonObject modObj = modulesObj.getAsJsonObject(modName);

                // Load module toggle state
                if (modObj.has("toggled")) {
                    boolean toggled = modObj.get("toggled").getAsBoolean();
                    if (toggled && !module.isToggled()) {
                        module.setToggled(true);
                    } else if (!toggled && module.isToggled()) {
                        module.setToggled(false);
                    }
                }

                // Load individual settings
                if (modObj.has("Settings")) {
                    JsonObject settingsObj = modObj.getAsJsonObject("Settings");

                    for (Setting setting : module.getSList()) {
                        if (!settingsObj.has(setting.getName())) continue;

                        JsonElement element = settingsObj.get(setting.getName());

                        if (setting instanceof BooleanSetting bs && !element.isJsonObject()) {
                            bs.setState(element.getAsBoolean());
                        } else if (setting instanceof DoubleSetting ds && !element.isJsonObject()) {
                            ds.setVal(element.getAsDouble());
                        } else if (setting instanceof ModeSetting ms && !element.isJsonObject()) {
                            ms.setCurrentMode(element.getAsString());
                        } else if (setting instanceof KeySetting ks && !element.isJsonObject()) {
                            ks.setKey(element.getAsInt());
                        } else if (setting instanceof ColorSetting cs && element.isJsonObject()) {
                            JsonObject colorObj = element.getAsJsonObject();
                            if (colorObj.has("packed")) cs.fromPackedARGB(colorObj.get("packed").getAsInt());
                            if (colorObj.has("rainbow")) cs.setRainbow(colorObj.get("rainbow").getAsBoolean());
                        }
                    }
                }
            }
            FukntClientClient.LOGGER.info("Successfully loaded config.");
        } catch (Exception e) {
            FukntClientClient.LOGGER.error("Failed to load config!", e);
        }
    }
}