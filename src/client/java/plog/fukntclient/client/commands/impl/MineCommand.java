package plog.fukntclient.client.commands.impl;

import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

import java.util.Optional;

@CommandInfo(name = "mine", usage = "#mine <block>  |  #mine stop", description = "Mine the nearest block of a given type", aliases = {"dig"})
public class MineCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length == 0) throw new CommandException("Usage: " + getUsage());

        if (args[0].equalsIgnoreCase("stop")) {
            FukntClientClient.Charlie.stop();
            sendMessage("§aCharlie stopped mining.");
            return;
        }

        // Accept "diamond_ore" or "minecraft:diamond_ore"
        String input = args[0].toLowerCase();
        if (!input.contains(":")) input = "minecraft:" + input;

        ResourceLocation rl = ResourceLocation.tryParse(input);
        if (rl == null) {
            throw new CommandException("Invalid block name: " + args[0]);
        }

        // Look up the block in the registry
        if (!BuiltInRegistries.BLOCK.containsKey(rl)) {
            throw new CommandException("Unknown block: " + args[0]);
        }

        Block block = BuiltInRegistries.BLOCK.get(rl).get().value();
        FukntClientClient.Charlie.mine(block);
        sendMessage("§aMining §f" + rl.getPath().replace("_", " ") + "§a...");
    }
}