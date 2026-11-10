package plog.fukntclient.client.commands;

import lombok.Getter;
import org.reflections.Reflections;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.exeption.CommandException;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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

        // FukntClient owns every "#" message. Baritone's own chat control is disabled
        // (see BaritoneBridge), so anything we don't know is reported here instead of
        // being sent to the server as chat.
        Optional<Command> command = getCommand(args[0]);
        if (command.isEmpty()) {
            Command.sendMessage("§cUnknown command §f" + PREFIX + args[0] + "§c. Type " + PREFIX + "fhelp for FukntClient commands");
            return true;
        }

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

    // ---------------------------------------------------------------------------
    // Command suggestions
    // ---------------------------------------------------------------------------

    /**
     * Builds chat suggestions for text that starts with {@link #PREFIX}.
     *
     * @param text   the full chat box contents
     * @param cursor the caret position inside {@code text}
     */
    public CompletableFuture<Suggestions> getSuggestions(String text, int cursor) {
        cursor = Math.max(PREFIX.length(), Math.min(cursor, text.length()));
        String upToCursor = text.substring(0, cursor);
        String body = upToCursor.substring(PREFIX.length());

        int lastSpace = body.lastIndexOf(' ');
        int start = PREFIX.length() + lastSpace + 1;
        SuggestionsBuilder builder = new SuggestionsBuilder(upToCursor, start);
        String typed = upToCursor.substring(start);

        String[] parts = body.split(" ", -1);
        if (parts.length == 1) {
            // Completing the command name itself.
            Set<String> seen = new HashSet<>();
            commands.values().stream()
                    .sorted(Comparator.comparing(Command::getName))
                    .forEach(cmd -> {
                        if (matches(cmd.getName(), typed) && seen.add(cmd.getName())) {
                            builder.suggest(cmd.getName(), Component.literal(cmd.getDescription()));
                        }
                    });
            commands.values().stream()
                    .sorted(Comparator.comparing(Command::getName))
                    .forEach(cmd -> cmd.getAliases().forEach(alias -> {
                        if (matches(alias, typed) && seen.add(alias)) {
                            builder.suggest(alias, Component.literal("Alias of " + PREFIX + cmd.getName()));
                        }
                    }));
            return builder.buildFuture();
        }

        Optional<Command> command = getCommand(parts[0]);
        if (command.isPresent()) {
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            try {
                List<String> options = command.get().suggest(args);
                if (options != null) {
                    new LinkedHashSet<>(options).stream()
                            .filter(o -> matches(o, typed))
                            .forEach(builder::suggest);
                }
            } catch (RuntimeException e) {
                FukntClientClient.LOGGER.debug("Suggestion failure for {}", parts[0], e);
            }
        }
        return builder.buildFuture();
    }

    /**
     * Same matching rule vanilla uses: the candidate matches if it, or any
     * word inside it (split on _ : / . -), starts with what was typed.
     */
    private static boolean matches(String candidate, String typed) {
        if (typed.isEmpty()) return true;
        String c = candidate.toLowerCase(Locale.ROOT);
        String t = typed.toLowerCase(Locale.ROOT);
        if (c.startsWith(t)) return true;
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if ((ch == '_' || ch == ':' || ch == '/' || ch == '.' || ch == '-') && c.startsWith(t, i + 1)) return true;
        }
        return false;
    }
}
