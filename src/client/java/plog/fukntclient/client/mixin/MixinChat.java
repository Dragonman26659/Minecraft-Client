package plog.fukntclient.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.impl.Player.LinkSupressor;

@Mixin(ChatComponent.class)
public class MixinChat {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onAddMessage(Component message, MessageSignature signature,
                              net.minecraft.client.GuiMessageTag tag, CallbackInfo ci) {

        // 1. Module suppression
        LinkSupressor mod = (LinkSupressor) FukntClientClient.mm.getModule(LinkSupressor.class);
        if (mod != null && mod.isToggled() && mod.shouldSuppress(message)) {
            ci.cancel();
            return;
        }

        //// 2. Blocked player suppression
        //String rawText = message.getString().toLowerCase();
        //for (String blockedName : FukntClientClient.fm.getBlocked()) {
        //    // Checks for standard vanilla chat "<PlayerName> msg" or system format "PlayerName msg"
        //    if (rawText.contains("<" + blockedName + ">") || rawText.startsWith(blockedName + " ")) {
        //        ci.cancel();
        //        return;
        //    }
        //}
    }
}