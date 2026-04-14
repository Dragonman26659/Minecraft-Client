package plog.fukntclient.client.mixin;

import net.minecraft.network.protocol.Packet;

// IMixinClientConnection.java
public interface IMixinClientConnection {
    void fuknt$sendDirect(Packet<?> packet);
}