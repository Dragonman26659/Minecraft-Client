package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;

@ModuleInfo(name = "Fullbright", description = "Maximum brightness", category = Category.Visual)
public class FullBright extends Module {

    @Override
    public void onTick() {
        if (FukntClientClient.mc.player == null) return;

        // Constantly refresh night vision so it never expires
        FukntClientClient.mc.player.addEffect(
                new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false)
        );
    }

    @Override
    public void onDisable() {
        if (FukntClientClient.mc.player == null) return;
        FukntClientClient.mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
}