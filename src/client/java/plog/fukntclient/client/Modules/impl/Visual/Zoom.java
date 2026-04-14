package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.client.Minecraft;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "Zoom", description = "Reduces FOV while active", category = Category.Visual)
public class Zoom extends Module {

    public final DoubleSetting zoomDivisor = new DoubleSetting("Zoom Level", 4, 1.5, 10, 0.1);

    public Zoom() {
        addSetting(zoomDivisor);
    }
}