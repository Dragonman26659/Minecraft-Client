package plog.fukntclient.client.Util;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.mixin.IMixinClientConnection;

public class PacketUtil {

    public static void send(Packet<?> packet) {
        Connection connection = FukntClientClient.mc.getConnection().getConnection();
        ((IMixinClientConnection) connection).fuknt$sendDirect(packet);
    }

    public static void sendMultiple(Packet<?>... packets) {
        for (Packet<?> packet : packets) send(packet);
    }
}