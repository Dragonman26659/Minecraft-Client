package plog.fukntclient.client.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plog.fukntclient.client.FukntClientClient;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        String username = playerInfo.getProfile().name();

        if (FukntClientClient.fm.isFriend(username)) {
            Component originalName = cir.getReturnValue();
            MutableComponent newName;

            if (originalName == null) {
                newName = Component.literal(username);
            } else {
                newName = originalName.copy();
            }


            newName.withStyle(ChatFormatting.AQUA);
            cir.setReturnValue(newName);
        }
    }
}
