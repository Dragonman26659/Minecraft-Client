package plog.fukntclient.client.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.events.PacketEvent;



@Mixin(Connection.class)
public class MixinClientConnection {
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        PacketEvent.Send event = FukntClientClient.EVENT_BUS.fireSend(new PacketEvent.Send(packet));
        if (event.cancelled) ci.cancel();
        else if (event.packet != packet) {
            ((Connection)(Object)this).send(event.packet);
            ci.cancel();
        }
    }

    @Unique
    public void fuknt$sendDirect(Packet<?> packet) {
        ((Connection)(Object)this).send(packet, null);
    }
}