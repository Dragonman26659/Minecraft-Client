package plog.fukntclient.client.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plog.fukntclient.client.FukntClientClient;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        String username = playerInfo.getProfile().name();

        List<ChatFormatting> activeFormats = new ArrayList<>();

        if (FukntClientClient.fm.isFriend(username)) activeFormats.add(ChatFormatting.AQUA);
        if (FukntClientClient.fm.isEnemy(username)) activeFormats.add(ChatFormatting.RED);
        if (FukntClientClient.fm.isBlocked(username)) activeFormats.add(ChatFormatting.YELLOW);

        if (!activeFormats.isEmpty()) {
            Component originalName = cir.getReturnValue();
            MutableComponent newName;

            if (originalName == null) {
                newName = Component.literal(username);
            } else {
                newName = originalName.copy();
            }

            // If they belong to multiple lists, alternate colors based on system time (every 1 second)
            int index = (int) ((System.currentTimeMillis() / 1000) % activeFormats.size());
            newName.withStyle(activeFormats.get(index));

            cir.setReturnValue(newName);
        }
    }
}