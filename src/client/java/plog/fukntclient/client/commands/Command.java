package plog.fukntclient.client.commands;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.Validate;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.exeption.CommandException;

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
}