package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "stop", usage = "#stop", description = "Stop Charlie (pathfinding / mining)", aliases = {"halt", "cancel"})
public class StopCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        FukntClientClient.Charlie.stop();
        sendMessage("§aCharlie halted.");
    }
}