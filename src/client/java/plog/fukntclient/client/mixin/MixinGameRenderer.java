package plog.fukntclient.client.mixin;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.impl.Visual.Zoom;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        Zoom zoom = (Zoom) FukntClientClient.mm.getModule(Zoom.class);
        if (zoom != null && zoom.isToggled()) {
            cir.setReturnValue((float) (cir.getReturnValue() / zoom.zoomDivisor.getVal()));
        }
    }
}