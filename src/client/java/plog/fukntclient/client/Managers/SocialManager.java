package plog.fukntclient.client.Managers;

import lombok.Getter;
import plog.fukntclient.client.FukntClientClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class SocialManager {

    @Getter private final Set<String> friends = new HashSet<>();
    @Getter private final Set<String> enemies = new HashSet<>();
    @Getter private final Set<String> blocked = new HashSet<>();

    private final Path saveFile;
    private final Gson gson = new Gson();

    public SocialManager() {
        saveFile = Path.of("fuknt/social.json");
        load();
    }

    // --- Friends ---
    public void addFriend(String name) { friends.add(name.toLowerCase()); save(); }
    public void removeFriend(String name) { friends.remove(name.toLowerCase()); save(); }
    public boolean isFriend(String name) { return friends.contains(name.toLowerCase()); }

    // --- Enemies ---
    public void addEnemy(String name) { enemies.add(name.toLowerCase()); save(); }
    public void removeEnemy(String name) { enemies.remove(name.toLowerCase()); save(); }
    public boolean isEnemy(String name) { return enemies.contains(name.toLowerCase()); }

    // --- Blocked ---
    public void addBlocked(String name) { blocked.add(name.toLowerCase()); save(); }
    public void removeBlocked(String name) { blocked.remove(name.toLowerCase()); save(); }
    public boolean isBlocked(String name) { return blocked.contains(name.toLowerCase()); }

    // --- Save / Load Logic ---
    private static class SocialData {
        Set<String> friends = new HashSet<>();
        Set<String> enemies = new HashSet<>();
        Set<String> blocked = new HashSet<>();
    }

    public void save() {
        try {
            Files.createDirectories(saveFile.getParent());
            SocialData data = new SocialData();
            data.friends = this.friends;
            data.enemies = this.enemies;
            data.blocked = this.blocked;

            try (Writer writer = Files.newBufferedWriter(saveFile)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to save social data", e);
        }
    }

    public void load() {
        if (!Files.exists(saveFile)) return;
        try (Reader reader = Files.newBufferedReader(saveFile)) {
            Type type = new TypeToken<SocialData>(){}.getType();
            SocialData loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                if (loaded.friends != null) friends.addAll(loaded.friends);
                if (loaded.enemies != null) enemies.addAll(loaded.enemies);
                if (loaded.blocked != null) blocked.addAll(loaded.blocked);
            }
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to load social data", e);
        }
    }
}