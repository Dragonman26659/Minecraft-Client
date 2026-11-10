package plog.fukntclient.client.commands;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.exeption.CommandException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public abstract class Command {

    protected static final Minecraft mc = FukntClientClient.mc;

    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;

    public Command() {
        CommandInfo info = getClass().getAnnotation(CommandInfo.class);
        Validate.notNull(info, "CONFUSED ANNOTATION EXCEPTION");

        this.name        = info.name();
        this.description = info.description();
        this.usage       = info.usage();
        this.aliases     = Arrays.asList(info.aliases());
    }

    public boolean isAlias(String str) {
        return aliases.stream().anyMatch(s -> s.equalsIgnoreCase(str));
    }

    public static void sendMessage(String msg) {
        if (FukntClientClient.mc.player != null) {
            FukntClientClient.mc.gui.getChat().addMessage(
                    Component.literal("§8[§cfuknt§8] §7" + msg)
            );
        }
    }

    public abstract void execute(String... args) throws CommandException;

    /**
     * Tab-completion candidates for this command.
     *
     * @param args everything typed after the command name, split on spaces. The
     *             last element is the (possibly empty) word currently being typed.
     *             The caller filters the result against that last word.
     */
    public List<String> suggest(String[] args) {
        return List.of();
    }

    // ---- helpers for subclasses -------------------------------------------------

    protected static List<String> onlinePlayers() {
        List<String> names = new ArrayList<>();
        if (mc.getConnection() == null) return names;
        mc.getConnection().getOnlinePlayers().forEach(info -> names.add(info.getProfile().name()));
        return names;
    }

    /** Current block coordinate for axis 0/1/2 (x/y/z), or an empty list if not in a world. */
    protected static List<String> playerCoord(int axis) {
        if (mc.player == null) return List.of();
        int v = axis == 0 ? mc.player.getBlockX() : axis == 1 ? mc.player.getBlockY() : mc.player.getBlockZ();
        return List.of(String.valueOf(v));
    }
}