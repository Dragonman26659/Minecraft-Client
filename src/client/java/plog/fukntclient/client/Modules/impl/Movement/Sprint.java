package plog.fukntclient.client.Modules.impl.Movement;

import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;

// Sprint.java
@ModuleInfo(name = "Sprint", description = "Auto sprints", category = Category.Movement)
public class Sprint extends Module {
    @Override
    public void onTick() {
        if (FukntClientClient.mc.player != null)
            FukntClientClient.mc.player.setSprinting(true);
    }
}