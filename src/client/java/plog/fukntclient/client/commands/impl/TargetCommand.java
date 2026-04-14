package plog.fukntclient.client.commands.impl;

import net.minecraft.core.BlockPos;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "target", usage = "#target <x> <y> <z> (or .target clear)", description = "Sets a HUD target destination", aliases = {"settarget", "goal"})
public class TargetCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            FukntClientClient.tm.setTargetPos(null);
            sendMessage("§aTarget cleared.");
            return;
        }

        if (args.length < 3) {
            throw new CommandException("Usage: " + getUsage());
        }

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);

            FukntClientClient.tm.setTargetPos(new BlockPos(x, y, z));
            sendMessage(String.format("§aTarget set to §f%d, %d, %d", x, y, z));
        } catch (NumberFormatException e) {
            throw new CommandException("Coordinates must be integers!");
        }
    }
}