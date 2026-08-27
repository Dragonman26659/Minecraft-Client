package plog.fukntclient.client.Managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GuiStateManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "fukntclient_gui_state.json";
    private static GuiStateData data;

    public static class ModuleState {
        public boolean extended = false;
    }

    public static class CategoryState {
        public int x, y;
        public boolean hasPosition = false;
        public boolean extended = true;
        public Map<String, ModuleState> modules = new HashMap<>();
    }

    public static class GuiStateData {
        public Map<String, CategoryState> categories = new HashMap<>();
    }

    private static File getFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/" + FILE_NAME);
    }

    private static GuiStateData get() {
        if (data == null) load();
        return data;
    }

    public static CategoryState getCategory(String name) {
        return get().categories.computeIfAbsent(name, k -> new CategoryState());
    }

    public static ModuleState getModule(String categoryName, String moduleName) {
        CategoryState cs = getCategory(categoryName);
        return cs.modules.computeIfAbsent(moduleName, k -> new ModuleState());
    }

    public static void load() {
        File f = getFile();
        if (!f.exists()) {
            data = new GuiStateData();
            return;
        }
        try (FileReader reader = new FileReader(f)) {
            GuiStateData loaded = GSON.fromJson(reader, GuiStateData.class);
            data = (loaded != null) ? loaded : new GuiStateData();
        } catch (IOException e) {
            data = new GuiStateData();
        }
    }

    public static void save() {
        if (data == null) return;
        File f = getFile();
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (FileWriter writer = new FileWriter(f)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }
}