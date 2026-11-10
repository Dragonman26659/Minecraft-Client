package plog.fukntclient.client.commands;

import lombok.Getter;
import org.reflections.Reflections;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.exeption.CommandException;

import java.util.*;

@Getter
public final class CommandManager {

    public static final String PREFIX = "#";
    private final Map<String, Command> commands = new HashMap<>();

    public CommandManager() {
        register();
    }

    private void register() {
        Reflections reflections = new Reflections("plog.fukntclient.client.commands.impl");
        Set<Class<? extends Command>> classes = reflections.getSubTypesOf(Command.class);

        for (Class<? extends Command> aClass : classes) {
            try {
                Command cmd = aClass.newInstance();
                commands.put(cmd.getName(), cmd);
                FukntClientClient.LOGGER.info("Command {} loaded", aClass.getSimpleName());
            } catch (InstantiationException | IllegalAccessException e) {
                FukntClientClient.LOGGER.error("Failed to load command: {}", aClass.getSimpleName(), e);
            }
        }
    }

    public boolean handleCommand(String message) {
        FukntClientClient.LOGGER.debug("Command Message Detected");

        if (message.isEmpty() || !message.startsWith(PREFIX)) return false;

        String[] args = message.substring(PREFIX.length()).split(" ");

        if (args.length == 0 || args[0].isEmpty()) {
            Command.sendMessage("Type " + PREFIX + "fhelp for FukntClient commands");
            return true;
        }

        // Preserve Baritone's native command names where FukntClient has a
        // similarly named convenience command. #goto mark/stop remain Charlie commands.
        String name = args[0].toLowerCase(Locale.ROOT);
        if (name.equals("help") || name.equals("mine")) return false;
        if (name.equals("goto") && (args.length < 2
                || (!args[1].equalsIgnoreCase("mark") && !args[1].equalsIgnoreCase("stop")))) {
            return false;
        }

        Optional<Command> command = getCommand(args[0]);
        // Unknown commands must be allowed through to Baritone's chat listener.
        if (command.isEmpty()) return false;

        try {
            command.get().execute(Arrays.copyOfRange(args, 1, args.length));
            return true;
        } catch (CommandException e) {
            Command.sendMessage("§c" + e.getMessage());
            return true;
        }
    }

    public Optional<Command> getCommand(String name) {
        Command direct = commands.get(name.toLowerCase());
        if (direct != null) return Optional.of(direct);

        return commands.values().stream()
                .filter(cmd -> cmd.isAlias(name))
                .findFirst();
    }
}
