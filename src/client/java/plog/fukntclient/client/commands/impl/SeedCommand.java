package plog.fukntclient.client.commands.impl;

import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;
import plog.fukntclient.client.Managers.SeedManager;

@CommandInfo(name = "seed", usage = ".seed <value>", description = "Sets the world seed for local simulation")
public class SeedCommand extends Command {
    @Override
    public void execute(String... args) throws CommandException {
        if (args.length < 1) throw new CommandException("Usage: " + getUsage());
        try {
            long seed = Long.parseLong(args[0]);
            SeedManager.setSeed(seed);
            sendMessage("§aSeed set to: §f" + seed);
        } catch (NumberFormatException e) {
            throw new CommandException("Invalid seed format.");
        }
    }
}