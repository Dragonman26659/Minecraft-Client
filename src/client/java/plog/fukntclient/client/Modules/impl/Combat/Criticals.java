package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

@ModuleInfo(name = "Criticals", description = "Makes every hit a critical hit", category = Category.Combat)
public class Criticals extends Module {

    public final ModeSetting mode = new ModeSetting("Mode",
            "Packet",
            "Jump",
            "MiniJump"
    );

    public final BooleanSetting requireFullSwing = new BooleanSetting("Require Full Swing", true);

    private int jumpCooldown = 0;

    @Override
    public void onEnable() {
        jumpCooldown = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.gameMode == null) return;

        if (!mc.options.keyAttack.isDown()) return;

        if (requireFullSwing.isActive() && mc.player.getAttackStrengthScale(0.0f) < 1.0f) return;

        if (jumpCooldown > 0) {
            jumpCooldown--;
            return;
        }

        if (!mc.player.onGround()) return;
        if (mc.player.isInWater() || mc.player.isInLava()) return;
        if (mc.player.onClimbable()) return;

        switch (mode.getCurrentMode()) {
            case "Packet"   -> doPacketCrit(mc);
            case "Jump"     -> doJumpCrit(mc, 0.42f);
            case "MiniJump" -> doJumpCrit(mc, 0.1f);
        }
    }

    private void doPacketCrit(Minecraft mc) {
        if (mc.getConnection() == null) return;

        assert mc.player != null;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        float  yaw   = mc.player.getYRot();
        float  pitch = mc.player.getXRot();

        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                x, y + 0.11, z, yaw, pitch, false, false));

        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                x, y,        z, yaw, pitch, true,  false));
    }


    private void doJumpCrit(Minecraft mc, float strength) {
        assert mc.player != null;
        mc.player.setDeltaMovement(
                mc.player.getDeltaMovement().x,
                strength,
                mc.player.getDeltaMovement().z
        );

        jumpCooldown = (strength > 0.3f) ? 6 : 3;
    }
}