package plog.fukntclient.client.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import plog.fukntclient.client.FukntClientClient;

@Mixin(ChatScreen.class)
public class MixinChatScreen {

    @Shadow private CommandSuggestions commandSuggestions;

    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onHandleChatInput(String message, boolean addToHistory, CallbackInfo ci) {
        if (FukntClientClient.cm.handleCommand(message)) ci.cancel();
    }


}
