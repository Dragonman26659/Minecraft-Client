package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "friend", usage = ".friend <add|remove|list> [name]", description = "Manage friends", aliases = {"f", "fr"})
public class FriendCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 0) throw new CommandException("Usage: " + getUsage());

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) throw new CommandException("Specify a player name");
                FukntClientClient.fm.addFriend(args[1]);
                sendMessage("§a+" + args[1] + " added as friend");
            }
            case "remove" -> {
                if (args.length < 2) throw new CommandException("Specify a player name");
                FukntClientClient.fm.removeFriend(args[1]);
                sendMessage("§c-" + args[1] + " removed from friends");
            }
            case "list" -> {
                if (FukntClientClient.fm.getFriends().isEmpty()) {
                    sendMessage("No friends added");
                    return;
                }
                FukntClientClient.fm.getFriends().forEach(f -> sendMessage("§b" + f));
            }
            default -> throw new CommandException("Usage: " + getUsage());
        }
    }
}
