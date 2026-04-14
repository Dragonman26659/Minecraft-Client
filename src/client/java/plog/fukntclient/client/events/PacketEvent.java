package plog.fukntclient.client.events;

import net.minecraft.network.protocol.Packet;

public class PacketEvent {
    public Packet<?> packet;
    public boolean cancelled = false;

    public PacketEvent(Packet<?> packet) {
        this.packet = packet;
    }

    public static class Send extends PacketEvent {
        public Send(Packet<?> packet) { super(packet); }
    }

    public static class Receive extends PacketEvent {
        public Receive(Packet<?> packet) { super(packet); }
    }
}