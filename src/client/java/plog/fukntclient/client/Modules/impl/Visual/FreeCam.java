package plog.fukntclient.client.Modules.impl.Visual;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.events.PacketEvent;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "FreeCam", description = "Spectator-style movement (Client Side)", category = Category.Visual)
public class FreeCam extends Module {

    private double oldX, oldY, oldZ;
    private float oldYaw, oldPitch;
    private GameType oldGameMode;

    public final DoubleSetting speed = new DoubleSetting("speed", 0.5, 0.1, 2.0, 0.1);

    public FreeCam() {
        addSetting(speed);
    }

    @Override
    public void onEnable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.gameMode == null) return;

        // Store original state
        oldX = mc.player.getX();
        oldY = mc.player.getY();
        oldZ = mc.player.getZ();
        oldYaw = mc.player.getYRot();
        oldPitch = mc.player.getXRot();
        oldGameMode = mc.gameMode.getPlayerMode();

        // Set local game mode to Spectator
        mc.gameMode.setLocalMode(GameType.SPECTATOR);
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.gameMode == null) return;

        // Restore original game mode
        if (oldGameMode != null) {
            mc.gameMode.setLocalMode(oldGameMode);
        }

        // Snap back to real server position
        mc.player.setPos(oldX, oldY, oldZ);
        mc.player.setYRot(oldYaw);
        mc.player.setXRot(oldPitch);
        mc.player.setDeltaMovement(0, 0, 0);
    }

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        // Prevent the server from knowing you are moving or changing abilities
        if (event.packet instanceof ServerboundMovePlayerPacket ||
                event.packet instanceof ServerboundPlayerAbilitiesPacket) {
            event.cancelled = true;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Apply custom speed setting to flying speed
        mc.player.getAbilities().setFlyingSpeed((float) (speed.getVal() / 5));
    }
}