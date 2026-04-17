package plog.fukntclient.client.commands.impl;

import net.minecraft.client.Minecraft;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Managers.LocationData;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

import java.util.Map;

@CommandInfo(name = "mark", usage = "#mark <save|show|target|remove> [name]", description = "Manage dimension-aware waypoints", aliases = {"marks", "waypoint"})
public class MarkCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 0) {
            throw new CommandException("Usage: " + getUsage());
        }

        String action = args[0].toLowerCase();
        Minecraft mc = FukntClientClient.mc;

        switch (action) {
            case "show":
                Map<String, LocationData> marks = FukntClientClient.markManager.getCurrentMarks();
                if (marks.isEmpty()) {
                    sendMessage("§cNo marks saved for this world/server.");
                    return;
                }
                sendMessage("§aSaved Marks:");
                marks.forEach((name, loc) -> {
                    String dimName = loc.getDimension().replace("minecraft:", "");
                    sendMessage(String.format("§7- §f%s §7(§a%d, %d, %d§7) [%s]", name, loc.getX(), loc.getY(), loc.getZ(), dimName));
                });
                break;

            case "save":
                if (args.length < 2) throw new CommandException("Usage: #mark save <name>");
                if (mc.player == null || mc.level == null) return;

                String saveName = args[1];
                int x = mc.player.getBlockX();
                int y = mc.player.getBlockY();
                int z = mc.player.getBlockZ();
                String dim = mc.level.dimension().location().toString();

                FukntClientClient.markManager.addMark(saveName, new LocationData(x, y, z, dim));
                sendMessage("§aSaved mark §f" + saveName + " §aat §f" + x + ", " + y + ", " + z);
                break;

            case "target":
                if (args.length < 2) throw new CommandException("Usage: #mark target <name>");
                String targetName = args[1];
                LocationData markLoc = FukntClientClient.markManager.getMark(targetName);

                if (markLoc == null) {
                    sendMessage("§cMark not found!");
                    return;
                }

                FukntClientClient.tm.setTarget(markLoc);
                sendMessage("§aTarget set to mark §f" + targetName);
                break;

            case "remove":
                if (args.length < 2) throw new CommandException("Usage: #mark remove <name>");
                String removeName = args[1];
                if (FukntClientClient.markManager.getMark(removeName) == null) {
                    sendMessage("§cMark not found!");
                    return;
                }
                FukntClientClient.markManager.removeMark(removeName);
                sendMessage("§aRemoved mark §f" + removeName);
                break;

            default:
                throw new CommandException("Unknown action. Usage: " + getUsage());
        }
    }
}