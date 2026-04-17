package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "goto", usage = "#goto <x> <y> <z>  |  #goto stop", description = "Path to coords", aliases = {"go", "path"})
public class GotoCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
    }
}
