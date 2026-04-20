package plog.fukntclient.client.commands.impl;

import net.minecraft.core.BlockPos;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Managers.LocationData;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(name = "goto", usage = "#goto <x> <y> <z>  |  #goto mark <name>  |  #goto stop", description = "Path to coords or a saved mark", aliases = {"go", "path"})
public class GotoCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 0) throw new CommandException("Usage: " + getUsage());

        // #goto stop
        if (args[0].equalsIgnoreCase("stop")) {
            FukntClientClient.Charlie.stop();
            sendMessage("§aCharlie stopped.");
            return;
        }

        // #goto mark <name>
        if (args[0].equalsIgnoreCase("mark")) {
            if (args.length < 2) throw new CommandException("Usage: #goto mark <name>");

            String markName = args[1];
            LocationData loc = FukntClientClient.markManager.getMark(markName);
            if (loc == null) {
                sendMessage("§cMark §f" + markName + " §cnot found.");
                return;
            }

            if (FukntClientClient.mc.level == null) return;
            String currentDim = FukntClientClient.mc.level.dimension().location().toString();

            // Use TargetManager to handle nether/overworld coordinate scaling
            FukntClientClient.tm.setTarget(loc);
            BlockPos scaled = FukntClientClient.tm.getScaledPos(currentDim);

            if (scaled == null) {
                sendMessage("§cCould not resolve position for current dimension.");
                return;
            }

            FukntClientClient.Charlie.GoTo(scaled);
            String dimName = loc.getDimension().replace("minecraft:", "");
            sendMessage(String.format("§aNavigating to mark §f%s §7(§a%d, %d, %d §7[%s]§7)",
                    markName, loc.getX(), loc.getY(), loc.getZ(), dimName));
            return;
        }

        // #goto <x> <y> <z>
        if (args.length < 3) throw new CommandException("Usage: " + getUsage());
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            FukntClientClient.Charlie.GoTo(new BlockPos(x, y, z));
            sendMessage(String.format("§aNavigating to §f%d, %d, %d", x, y, z));
        } catch (NumberFormatException e) {
            throw new CommandException("Coordinates must be integers!");
        }
    }
}