package plog.fukntclient.client.Managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import plog.fukntclient.client.FukntClientClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MarkManager {

    // Structure: Map<Server IP or World Name, Map<Mark Name, LocationData>>
    private final Map<String, Map<String, LocationData>> serverMarks = new HashMap<>();
    private final Path saveFile;
    private final Gson gson = new Gson();

    public MarkManager() {
        saveFile = Path.of("fuknt/marks.json");
        load();
    }

    private String getServerId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServer() != null) {
            return mc.getCurrentServer().ip;
        }
        if (mc.getSingleplayerServer() != null) {
            return mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        return "singleplayer";
    }

    public Map<String, LocationData> getCurrentMarks() {
        return serverMarks.computeIfAbsent(getServerId(), k -> new HashMap<>());
    }

    public void addMark(String name, LocationData data) {
        getCurrentMarks().put(name.toLowerCase(), data);
        save();
    }

    public void removeMark(String name) {
        getCurrentMarks().remove(name.toLowerCase());
        save();
    }

    public LocationData getMark(String name) {
        return getCurrentMarks().get(name.toLowerCase());
    }

    public void save() {
        try {
            Files.createDirectories(saveFile.getParent());
            try (Writer writer = Files.newBufferedWriter(saveFile)) {
                gson.toJson(serverMarks, writer);
            }
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to save marks", e);
        }
    }

    public void load() {
        if (!Files.exists(saveFile)) return;
        try (Reader reader = Files.newBufferedReader(saveFile)) {
            Type type = new TypeToken<Map<String, Map<String, LocationData>>>(){}.getType();
            Map<String, Map<String, LocationData>> loaded = gson.fromJson(reader, type);
            if (loaded != null) serverMarks.putAll(loaded);
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to load marks", e);
        }
    }
}