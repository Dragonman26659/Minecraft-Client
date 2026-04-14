package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;



// TODO: Add option to automatically equip elytra
// FIX THE FLY DETECTION BYPASS
@ModuleInfo(name = "Fly", description = "Allows the player to fly", category = Category.Movement)
public class Fly extends Module {

    public final ModeSetting   mode       = new ModeSetting("Mode", "Elytra", "Packet", "Vanilla");
    public final DoubleSetting speed      = new DoubleSetting("Speed",        1.0, 0.1, 10.0, 0.1);
    public final DoubleSetting elytraSpeed= new DoubleSetting("Elytra Speed", 1.8, 0.1, 10.0, 0.1);
    public final BooleanSetting antiKick  = new BooleanSetting("Anti Kick",   true);
    public final BooleanSetting vertical  = new BooleanSetting("Vertical",    true);

    private int antiKickTimer = 0;

    public Fly() {
        addSettings(mode, speed, elytraSpeed, antiKick, vertical);
    }

    @Override
    public void onEnable() {
        antiKickTimer = 0;
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        mc.player.getAbilities().flying = false;
        mc.player.setDeltaMovement(0, 0, 0);

        if (mode.getCurrentMode().equals("Elytra")) {
            mc.player.stopFallFlying();
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        switch (mode.getCurrentMode()) {
            case "Elytra"  -> tickElytra(mc);
            case "Packet"  -> tickPacket(mc);
            case "Vanilla" -> tickVanilla(mc);
        }

        if (antiKick.isActive()) {
            antiKickTimer++;
            if (antiKickTimer >= 40) {
                if (mc.getConnection() != null) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            mc.player.getX(),
                            mc.player.getY() - 0.04,
                            mc.player.getZ(),
                            false,
                            false
                    ));
                }
                antiKickTimer = 0;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Vanilla
    // -------------------------------------------------------------------------

    private void tickVanilla(Minecraft mc) {
        mc.player.getAbilities().flying = true;
        mc.player.getAbilities().setFlyingSpeed((float) speed.getVal() * 0.05f);
    }

    // -------------------------------------------------------------------------
    // Packet
    // -------------------------------------------------------------------------

    private void tickPacket(Minecraft mc) {
        mc.player.getAbilities().flying = false;

        double spd = speed.getVal() * 0.1;
        double[] move = buildMoveVector(mc, spd);
        double dx = move[0], dz = move[1];

        double dy = 0;
        if (vertical.isActive()) {
            if (mc.options.keyJump.isDown())  dy =  spd;
            if (mc.options.keyShift.isDown()) dy = -spd;
        }

        mc.player.setDeltaMovement(dx, dy, dz);

        // Standard move packet logic
        sendGroundSpoof(mc);
    }

    // -------------------------------------------------------------------------
    // Elytra
    // -------------------------------------------------------------------------

    private void tickElytra(Minecraft mc) {
        if (!mc.player.isFallFlying() && mc.getConnection() != null) {
            mc.getConnection().send(new ServerboundPlayerCommandPacket(
                    mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
            ));
        }

        double spd = elytraSpeed.getVal() * 0.1;
        double[] move = buildMoveVector(mc, spd);
        double dx = move[0], dz = move[1];

        double dy = 0;
        if (vertical.isActive()) {
            if (mc.options.keyJump.isDown())  dy =  spd;
            if (mc.options.keyShift.isDown()) dy = -spd;
        }

        mc.player.setDeltaMovement(dx, dy, dz);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a [dx, dz] movement vector from WASD input, using the same
     * trig convention as Speed.java.
     */
    private double[] buildMoveVector(Minecraft mc, double spd) {
        float  yaw = mc.player.getYRot();
        double rad = Math.toRadians(yaw);

        double dx = 0, dz = 0;

        if (mc.options.keyUp.isDown())    { dx -= Math.sin(rad) * spd;  dz += Math.cos(rad) * spd; }
        if (mc.options.keyDown.isDown())  { dx += Math.sin(rad) * spd;  dz -= Math.cos(rad) * spd; }
        if (mc.options.keyLeft.isDown())  { dx += Math.cos(rad) * spd;  dz += Math.sin(rad) * spd; }
        if (mc.options.keyRight.isDown()) { dx -= Math.cos(rad) * spd;  dz -= Math.sin(rad) * spd; }

        return new double[]{ dx, dz };
    }

    private void sendGroundSpoof(Minecraft mc) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                mc.player.getX(),
                mc.player.getY(),
                mc.player.getZ(),
                mc.player.getYRot(),
                mc.player.getXRot(),
                true,   // onGround spoofed
                false   // horizontalCollision
        ));
    }
}