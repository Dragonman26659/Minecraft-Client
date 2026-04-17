package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

@ModuleInfo(name = "WTap", description = "Resets sprint on attack for extra knockback", category = Category.Combat)
public class wTap extends Module {

    public final ModeSetting mode = new ModeSetting("Mode",
            "Packet",   // Sends stop/start sprint packets — no visible stutter
            "Vanilla"   // Actually toggles sprinting client-side for a tick
    );

    public final DoubleSetting resetDelay = new DoubleSetting("Reset Delay", 0.0, 0.0, 5.0, 1.0);
    public final BooleanSetting requireFullSwing = new BooleanSetting("Require Full Swing", true);
    public final BooleanSetting requireSprint = new BooleanSetting("Require Sprint", true);
    public final BooleanSetting autoResprint = new BooleanSetting("Auto Resprint", true);

    private boolean didReset    = false;
    private int     delayTimer  = 0;
    private boolean wasAttacked = false;

    public wTap() {
        addSettings(mode, resetDelay, requireFullSwing, requireSprint, autoResprint);
    }

    @Override
    public void onEnable() {
        didReset    = false;
        delayTimer  = 0;
        wasAttacked = false;
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Restore sprint state so the player doesn't get stuck walking
        if (mc.player.isSprinting()) return;
        if (mc.options.keySprint.isDown() || mc.options.keyUp.isDown()) {
            sendSprintPacket(mc, true);
            mc.player.setSprinting(true);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Handle delayed reset — counts down after attack is detected
        if (delayTimer > 0) {
            delayTimer--;
            if (delayTimer == 0) {
                executeReset(mc);
            }
            return;
        }

        // Re-sprint after reset, once the player is moving again
        if (didReset && autoResprint.isActive()) {
            if (mc.options.keyUp.isDown()) {
                doResprint(mc);
                didReset = false;
            }
        }

        // Detect attack this tick
        boolean attacking = mc.options.keyAttack.isDown();
        if (!attacking) {
            wasAttacked = false;
            return;
        }
        if (wasAttacked) return; // already handled this press
        wasAttacked = true;

        // Guard conditions
        if (requireFullSwing.isActive() && mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;
        if (requireSprint.isActive() && !mc.player.isSprinting()) return;

        // Apply reset immediately or after a delay
        int delay = (int) resetDelay.getVal();
        if (delay > 0) {
            delayTimer = delay;
        } else {
            executeReset(mc);
        }
    }

    // -------------------------------------------------------------------------
    // Core reset
    // -------------------------------------------------------------------------

    private void executeReset(Minecraft mc) {
        switch (mode.getCurrentMode()) {
            case "Packet"  -> doPacketReset(mc);
            case "Vanilla" -> doVanillaReset(mc);
        }
        didReset = true;
    }

    /**
     * Sends STOP_SPRINTING then immediately START_SPRINTING.
     * The server registers the sprint break (extra knockback) without
     * any visible movement stutter on the client.
     */
    private void doPacketReset(Minecraft mc) {
        if (mc.getConnection() == null) return;

        sendSprintPacket(mc, false);
        sendSprintPacket(mc, true);
    }

    /**
     * Briefly stops actual client sprinting for one tick.
     * More detectable but produces a genuine sprint reset.
     */
    private void doVanillaReset(Minecraft mc) {
        mc.player.setSprinting(false);
        // autoResprint will re-enable it next tick once keyUp is held
    }

    // -------------------------------------------------------------------------
    // Re-sprint after reset
    // -------------------------------------------------------------------------

    private void doResprint(Minecraft mc) {
        switch (mode.getCurrentMode()) {
            case "Packet" -> {
                sendSprintPacket(mc, true);
                mc.player.setSprinting(true);
            }
            case "Vanilla" -> mc.player.setSprinting(true);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendSprintPacket(Minecraft mc, boolean start) {
        if (mc.getConnection() == null) return;
        ServerboundPlayerCommandPacket.Action action = start
                ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
                : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
        mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, action));
    }
}