package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.events.PacketEvent;

import java.lang.reflect.Field;

@ModuleInfo(name = "NoFall", description = "Cancels fall damage by spoofing ground state", category = Category.Movement)
public class NoFall extends Module {

    public NoFall() {}

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        var mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Check if the packet is a movement packet
        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            if (mc.player.fallDistance > 3.0f) {
                try {
                    Field onGroundField = ServerboundMovePlayerPacket.class.getDeclaredField("onGround");
                    onGroundField.setAccessible(true);
                    onGroundField.set(packet, true);

                    mc.player.fallDistance = 0;
                } catch (Exception e) {
                    FukntClientClient.LOGGER.error("NoFall reflection failed", e);
                }
            }
        }
    }
}