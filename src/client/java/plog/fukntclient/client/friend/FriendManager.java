package plog.fukntclient.client.friend;

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

public class FriendManager {

    @Getter
    private final Set<String> friends = new HashSet<>();
    private final Path saveFile;
    private final Gson gson = new Gson();

    public FriendManager() {
        saveFile = Path.of("fuknt/friends.json");
        load();
    }

    public void add(String name) {
        friends.add(name.toLowerCase());
        save();
    }

    public void remove(String name) {
        friends.remove(name.toLowerCase());
        save();
    }

    public boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public void save() {
        try {
            Files.createDirectories(saveFile.getParent());
            try (Writer writer = Files.newBufferedWriter(saveFile)) {
                gson.toJson(friends, writer);
            }
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to save friends", e);
        }
    }

    public void load() {
        if (!Files.exists(saveFile)) return;
        try (Reader reader = Files.newBufferedReader(saveFile)) {
            Type type = new TypeToken<HashSet<String>>(){}.getType();
            Set<String> loaded = gson.fromJson(reader, type);
            if (loaded != null) friends.addAll(loaded);
        } catch (IOException e) {
            FukntClientClient.LOGGER.error("Failed to load friends", e);
        }
    }
}
