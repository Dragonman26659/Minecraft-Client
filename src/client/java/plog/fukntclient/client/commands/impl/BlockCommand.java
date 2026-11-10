package plog.fukntclient.client.commands.impl;

import java.util.List;
import java.util.ArrayList;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "block", usage = "#block <add|remove> <name>", description = "Manage blocked players", aliases = {"ignore"})
public class BlockCommand extends Command {
    @Override
    public void execute(String... args) throws CommandException {
        if (args.length < 2) throw new CommandException("Usage: " + getUsage());

        String action = args[0].toLowerCase();
        String name = args[1];

        if (action.equals("add")) {
            FukntClientClient.fm.addBlocked(name);
            sendMessage("§eBlocked §f" + name + "§e.");
        } else if (action.equals("remove")) {
            FukntClientClient.fm.removeBlocked(name);
            sendMessage("§aUnblocked §f" + name + "§a.");
        } else {
            throw new CommandException("Usage: " + getUsage());
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("add", "remove");
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return onlinePlayers().stream()
                    .filter(n -> !FukntClientClient.fm.getBlocked().contains(n.toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return new ArrayList<>(FukntClientClient.fm.getBlocked());
        }
        return List.of();
    }
}
