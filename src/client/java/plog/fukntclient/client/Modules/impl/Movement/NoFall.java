package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.events.PacketEvent;

import java.lang.reflect.Field;

// NoFall.java
@ModuleInfo(name = "NoFall", description = "Cancels fall damage", category = Category.Movement)
public class NoFall extends Module {
    public NoFall() {}

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;
        if (FukntClientClient.mc.player == null) return;

        // Only spoof when we're actually falling and would take damage
        if (FukntClientClient.mc.player.fallDistance > 2.5f) {
            event.packet = new ServerboundMovePlayerPacket.StatusOnly(true, true);
        }
    }
}
