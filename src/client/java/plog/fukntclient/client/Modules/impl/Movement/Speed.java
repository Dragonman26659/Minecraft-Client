package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

@ModuleInfo(name = "Speed", description = "Makes the player move faster", category = Category.Movement)
public class Speed extends Module {

    public final ModeSetting   mode       = new ModeSetting("Mode", "BHop", "Standard", "Potion", "Strafe");
    public final DoubleSetting speed      = new DoubleSetting("Speed",        1.3, 0.1, 10.0, 0.1);
    public final DoubleSetting bhopBoost  = new DoubleSetting("BHop Boost",   0.4, 0.1,  2.0, 0.05);
    public final BooleanSetting onlyGround= new BooleanSetting("Only Ground", true);
    public final BooleanSetting autoJump  = new BooleanSetting("Auto Jump",   true);

    public Speed() {
        addSettings(mode, speed, bhopBoost, onlyGround, autoJump);
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;
        removeSpeedPotion(mc);
        mc.player.getAbilities().setWalkingSpeed(0.1f);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        if (onlyGround.isActive() && !mc.player.onGround()) return;

        switch (mode.getCurrentMode()) {
            case "BHop"     -> tickBhop(mc);
            case "Standard" -> tickStandard(mc);
            case "Potion"   -> tickPotion(mc);
            case "Strafe"   -> tickStrafe(mc);
        }
    }

    private void tickStandard(Minecraft mc) {
        mc.player.getAbilities().setWalkingSpeed((float) speed.getVal() * 0.1f);
    }

    private void tickBhop(Minecraft mc) {
        if (mc.player.onGround()) {
            float  yaw = mc.player.getYRot();
            double rad = Math.toRadians(yaw);
            double spd = speed.getVal() * 0.1 + bhopBoost.getVal() * 0.1;

            double dx = 0, dz = 0;
            if (mc.options.keyUp.isDown())    { dx -= Math.sin(rad) * spd;  dz += Math.cos(rad) * spd; }
            if (mc.options.keyDown.isDown())  { dx += Math.sin(rad) * spd;  dz -= Math.cos(rad) * spd; }
            if (mc.options.keyLeft.isDown())  { dx += Math.cos(rad) * spd;  dz += Math.sin(rad) * spd; }
            if (mc.options.keyRight.isDown()) { dx -= Math.cos(rad) * spd;  dz -= Math.sin(rad) * spd; }

            if (dx != 0 || dz != 0) {
                mc.player.setDeltaMovement(dx, mc.player.getDeltaMovement().y, dz);
                if (autoJump.isActive()) {
                    mc.player.setDeltaMovement(
                            mc.player.getDeltaMovement().x,
                            0.42,
                            mc.player.getDeltaMovement().z
                    );
                }
            }
        }
    }

    private void tickPotion(Minecraft mc) {
        // Apply a speed effect directly rather than using walkingSpeed,
        // so it syncs with the server's expected movement speed
        var effects = mc.player.getActiveEffects();
        boolean hasSpeed = effects.stream().anyMatch(e ->
                e.getEffect().value() == MobEffects.SPEED.value()
        );

        if (!hasSpeed) {
            mc.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SPEED,
                    Integer.MAX_VALUE,               // duration — lasts until disable
                    (int) speed.getVal() - 1,        // amplifier (0 = Speed I)
                    false, false                     // no particles, no icon
            ));
        }
    }

    private void tickStrafe(Minecraft mc) {
        // Strafe mode — boost only horizontal strafe keys, not forward/back
        if (!mc.player.onGround()) return;

        float  yaw = mc.player.getYRot();
        double rad = Math.toRadians(yaw);
        double spd = speed.getVal() * 0.1;
        double dx  = mc.player.getDeltaMovement().x;
        double dz  = mc.player.getDeltaMovement().z;

        if (mc.options.keyLeft.isDown())  { dx += Math.cos(rad) * spd; dz += Math.sin(rad) * spd; }
        if (mc.options.keyRight.isDown()) { dx -= Math.cos(rad) * spd; dz -= Math.sin(rad) * spd; }

        mc.player.setDeltaMovement(dx, mc.player.getDeltaMovement().y, dz);
    }

    private void removeSpeedPotion(Minecraft mc) {
        mc.player.removeEffect(net.minecraft.world.effect.MobEffects.SPEED);
    }
}