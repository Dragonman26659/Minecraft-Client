package plog.fukntclient.client.commands.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;

@CommandInfo(
        name = "locatetarget",
        usage = ".locatetarget <structure_name>",
        description = "Locates a structure and sets it as your HUD target",
        aliases = {"findstruct", "lt"}
)
public class LocateTargetCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {

    }
}