package plog.fukntclient.client.commands.impl;

import net.minecraft.core.BlockPos;
import plog.fukntclient.client.Util.BaritoneUtil;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "goto", usage = "#goto <x> <y> <z>  |  #goto stop", description = "Path to coords", aliases = {"go", "path"})
public class GotoCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (!BaritoneUtil.isAvailable())
            throw new CommandException("Baritone is not installed");

        if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            BaritoneUtil.stop();
            sendMessage("Baritone stopped");
            return;
        }

        if (args.length != 3) throw new CommandException("Usage: " + getUsage());

        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            BaritoneUtil.goTo(new BlockPos(x, y, z));
            sendMessage("Pathing to " + x + " " + y + " " + z);
        } catch (NumberFormatException e) {
            throw new CommandException("Coordinates must be integers");
        }
    }
}
