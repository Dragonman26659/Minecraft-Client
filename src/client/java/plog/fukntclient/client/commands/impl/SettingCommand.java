package plog.fukntclient.client.commands.impl;

import java.util.List;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.commands.Command;
import plog.fukntclient.client.commands.CommandInfo;
import plog.fukntclient.client.exeption.CommandException;
import plog.fukntclient.client.settings.Setting;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

@CommandInfo(name = "setting", usage = "#setting <module> <setting> <value>  (use _ for spaces in setting names)", description = "Change a module setting", aliases = {"s", "set"})
public class SettingCommand extends Command {

    @Override
    public void execute(String... args) throws CommandException {
        if (args.length != 3) throw new CommandException("Usage: " + getUsage());

        Module mod = FukntClientClient.mm.getModule(args[0]);
        if (mod == null) throw new CommandException("Module '" + args[0] + "' not found");

        Setting setting = mod.getSList().stream()
                .filter(s -> s.getName().equalsIgnoreCase(args[1])
                        || s.getName().replace(' ', '_').equalsIgnoreCase(args[1]))
                .findFirst()
                .orElseThrow(() -> new CommandException("Setting '" + args[1] + "' not found"));

        try {
            if (setting instanceof BooleanSetting bs) {
                if (!args[2].equalsIgnoreCase("true") && !args[2].equalsIgnoreCase("false"))
                    throw new CommandException("Value must be true or false");
                bs.setState(args[2].equalsIgnoreCase("true"));

            } else if (setting instanceof DoubleSetting ds) {
                double val = Double.parseDouble(args[2]);
                if (val < ds.getMinVal() || val > ds.getMaxVal())
                    throw new CommandException("Value must be between " + ds.getMinVal() + " and " + ds.getMaxVal());
                ds.setVal(val);

            } else if (setting instanceof ModeSetting ms) {
                if (ms.getModes().stream().noneMatch(m -> m.equalsIgnoreCase(args[2])))
                    throw new CommandException("Mode '" + args[2] + "' not found. Options: " + String.join(", ", ms.getModes()));
                ms.setCurrentMode(args[2]);
            }

            sendMessage("Set §b" + mod.getName() + "§7.§b" + setting.getName() + "§7 to §a" + args[2]);

        } catch (NumberFormatException e) {
            throw new CommandException("Invalid number: " + args[2]);
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return FukntClientClient.mm.getModules().values().stream().map(Module::getName).sorted().toList();
        }
        Module mod = FukntClientClient.mm.getModule(args[0]);
        if (mod == null) return List.of();

        // Only settings this command can actually change.
        if (args.length == 2) {
            return mod.getSList().stream()
                    .filter(s -> s instanceof BooleanSetting || s instanceof DoubleSetting || s instanceof ModeSetting)
                    .map(s -> s.getName().replace(' ', '_'))
                    .toList();
        }
        if (args.length == 3) {
            Setting setting = mod.getSList().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(args[1])
                            || s.getName().replace(' ', '_').equalsIgnoreCase(args[1]))
                    .findFirst().orElse(null);
            if (setting instanceof BooleanSetting) return List.of("true", "false");
            if (setting instanceof ModeSetting ms) return ms.getModes();
            if (setting instanceof DoubleSetting ds) return List.of(String.valueOf(ds.getVal()));
        }
        return List.of();
    }
}
