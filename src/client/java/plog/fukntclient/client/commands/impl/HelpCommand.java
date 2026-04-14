package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.commands.CommandManager;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "help", usage = ".help", description = "Lists all commands", aliases = {"h", "?"})
public class HelpCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        FukntClientClient.cm.getCommands().values().forEach(cmd ->
                sendMessage("§b" + CommandManager.PREFIX + cmd.getName()
                        + " §8- §7" + cmd.getDescription()
                        + " §8| §7" + cmd.getUsage())
        );
    }
}
