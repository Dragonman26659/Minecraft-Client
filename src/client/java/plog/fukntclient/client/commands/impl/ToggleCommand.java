package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "toggle", usage = ".toggle <module>", description = "Toggle a module", aliases = {"t", "tog"})
public class ToggleCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 0) throw new CommandException("Usage: " + getUsage());

        Module mod = FukntClientClient.mm.getModule(args[0]);
        if (mod == null) throw new CommandException("Module '" + args[0] + "' not found");

        mod.toggle();
        sendMessage(mod.getName() + " " + (mod.isToggled() ? "§aenabled" : "§cdisabled"));
    }
}
