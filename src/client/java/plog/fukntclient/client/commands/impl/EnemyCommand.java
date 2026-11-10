package plog.fukntclient.client.commands.impl;

import java.util.List;
import java.util.ArrayList;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "enemy", usage = "#enemy <add|remove> <name>", description = "Manage your enemies", aliases = {"enemies"})
public class EnemyCommand extends Command {
    @Override
    public void execute(String... args) throws CommandException {
        if (args.length < 2) throw new CommandException("Usage: " + getUsage());

        String action = args[0].toLowerCase();
        String name = args[1];

        if (action.equals("add")) {
            FukntClientClient.fm.addEnemy(name);
            sendMessage("§cAdded §f" + name + " §cto enemies.");
        } else if (action.equals("remove")) {
            FukntClientClient.fm.removeEnemy(name);
            sendMessage("§aRemoved §f" + name + " §afrom enemies.");
        } else {
            throw new CommandException("Usage: " + getUsage());
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("add", "remove");
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            return onlinePlayers().stream()
                    .filter(n -> !FukntClientClient.fm.getEnemies().contains(n.toLowerCase()))
                    .toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return new ArrayList<>(FukntClientClient.fm.getEnemies());
        }
        return List.of();
    }
}
