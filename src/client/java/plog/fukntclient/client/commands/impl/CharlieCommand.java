package plog.fukntclient.client.commands.impl;

import java.util.List;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;
import plog.charlie.CharlieGoapPlanner;

import java.util.Arrays;

@CommandInfo(name = "charlie", usage = "#charlie plan <goal> | #charlie chests | #charlie pvp [player] | #charlie stop", description = "Charlie GOAP survival planner and fast combat controller", aliases = {"ch"})
public class CharlieCommand extends Command {
    @Override public void execute(String... args) throws CommandException {
        if (args.length == 0) throw new CommandException(getUsage());
        String mode = args[0].toLowerCase();
        if (mode.equals("stop")) {
            FukntClientClient.Charlie.stop();
            sendMessage("Charlie stopped.");
            return;
        }
        if (mode.equals("chests")) {
            var chests = FukntClientClient.Charlie.findNearbyChests(16);
            if (chests.isEmpty()) sendMessage("No loaded chests found within 16 blocks.");
            else {
                sendMessage("Nearby chests (nearest first):");
                chests.stream().limit(12).forEach(pos -> sendMessage(pos.getX() + " " + pos.getY() + " " + pos.getZ()));
            }
            return;
        }
        if (mode.equals("pvp")) {
            String target = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
            FukntClientClient.Charlie.startCombat(target);
            sendMessage(target.isBlank() ? "Fast combat controller enabled; targeting nearest player." : "Fast combat controller targeting " + target + ".");
            return;
        }
        String goal;
        if (mode.equals("plan")) {
            if (args.length < 2) throw new CommandException("Usage: #charlie plan <goal>");
            goal = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        } else {
            goal = String.join(" ", args);
        }
        CharlieGoapPlanner.Plan plan = FukntClientClient.Charlie.plan(goal);
        for (String line : plan.lines()) sendMessage(line);
        sendMessage("Plan generated. Execution support for chest transfers, crafting and furnace automation is still being developed; existing #charliemine/#goto actions remain available.");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("plan", "chests", "pvp", "stop");
        if (args.length == 2 && args[0].equalsIgnoreCase("pvp")) return onlinePlayers();
        return List.of();
    }
}
