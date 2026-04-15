package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

// TODO: Add option to automatically equip elytra
@ModuleInfo(name = "Fly", description = "Allows the player to fly", category = Category.Movement)
public class Fly extends Module {

    public final ModeSetting    mode         = new ModeSetting("Mode", "Elytra", "Packet", "Vanilla");
    public final DoubleSetting  speed        = new DoubleSetting("Speed",         1.0, 0.1, 10.0, 0.1);
    public final DoubleSetting  elytraSpeed  = new DoubleSetting("Elytra Speed",  1.8, 0.1, 10.0, 0.1);
    public final BooleanSetting antiKick     = new BooleanSetting("Anti Kick",    true);
    public final BooleanSetting vertical     = new BooleanSetting("Vertical",     true);
    public final BooleanSetting instantFly   = new BooleanSetting("Instant Fly",  true);
    public final BooleanSetting speedCtrl    = new BooleanSetting("Speed Control", true);
    public final BooleanSetting heightCtrl   = new BooleanSetting("Height Control", false);
    public final BooleanSetting stopInWater  = new BooleanSetting("Stop In Water", true);

    private int antiKickTimer = 0;
    private int jumpTimer     = 0;

    public Fly() {
        addSettings(mode, speed, elytraSpeed, antiKick, vertical,
                instantFly, speedCtrl, heightCtrl, stopInWater);
    }

    @Override
    public void onEnable() {
        antiKickTimer = 0;
        jumpTimer     = 0;
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
        sendGroundSpoof(mc);
    }

    // -------------------------------------------------------------------------
    // Elytra
    // -------------------------------------------------------------------------

    private void tickElytra(Minecraft mc) {
        if (jumpTimer > 0)
            jumpTimer--;

        // Player must have an elytra equipped to glide
        if (!mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA))
            return;

        if (mc.player.isFallFlying()) {
            // Stop flying when entering water
            if (stopInWater.isActive() && mc.player.isInWater()) {
                sendStartStopPacket(mc);
                return;
            }

            elytraControlSpeed(mc);
            elytraControlHeight(mc);
            return;
        }

        // Not yet gliding — try instant-fly on jump press
        if (mc.options.keyJump.isDown())
            doInstantFly(mc);
    }

    /** Sends the START_FALL_FLYING toggle packet. */
    private void sendStartStopPacket(Minecraft mc) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(
                mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    /**
     * W/S accelerate / decelerate along the look direction.
     * scaled by the Elytra Speed setting.
     */
    private void elytraControlSpeed(Minecraft mc) {
        if (!speedCtrl.isActive()) return;

        float yaw     = (float) Math.toRadians(mc.player.getYRot());
        double scale  = elytraSpeed.getVal() * 0.05;
        Vec3 forward  = new Vec3(-Mth.sin(yaw) * scale, 0, Mth.cos(yaw) * scale);

        Vec3 v = mc.player.getDeltaMovement();
        if      (mc.options.keyUp.isDown())   mc.player.setDeltaMovement(v.add(forward));
        else if (mc.options.keyDown.isDown()) mc.player.setDeltaMovement(v.subtract(forward));
    }

    /**
     * Space/Shift adjust vertical velocity.
     */
    private void elytraControlHeight(Minecraft mc) {
        if (!heightCtrl.isActive()) return;

        Vec3 v     = mc.player.getDeltaMovement();
        boolean up = mc.options.keyJump.isDown();
        boolean dn = mc.options.keyShift.isDown();

        // Prevent entering sneak pose while we steal the shift key
        if (dn) mc.options.keyShift.setDown(false);

        if (up && !dn)       mc.player.setDeltaMovement(v.x, v.y + 0.08, v.z);
        else if (dn && !up)  mc.player.setDeltaMovement(v.x, v.y - 0.04, v.z);
    }

    /**
     * Launches the player into elytra glide without the vanilla double-jump.
     */
    private void doInstantFly(Minecraft mc) {
        if (!instantFly.isActive()) return;

        if (jumpTimer <= 0) {
            jumpTimer = 20;
            mc.player.setJumping(false);
            mc.player.setSprinting(true);
            mc.player.jumpFromGround();
        }

        sendStartStopPacket(mc);
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

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
                true,
                false
        ));
    }
}