package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.events.PacketEvent;
import plog.fukntclient.client.settings.impl.BooleanSetting;

@ModuleInfo(name = "Jesus", description = "Walk on water and lava with NCP bypass", category = Category.Movement)
public class Jesus extends Module {

    public final BooleanSetting bypass = new BooleanSetting("NoCheat+ bypass", false);
    public final BooleanSetting sneak = new BooleanSetting("Sneak to Sink", true);

    private int tickTimer = 10;
    private int packetTimer = 0;

    public Jesus() {
        addSettings(bypass, sneak);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Allow sinking by sneaking
        if (sneak.isActive() && mc.player.isShiftKeyDown()) return;

        // Move up if submerged
        if (mc.player.isInWater() || mc.player.isInLava()) {
            Vec3 velocity = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(velocity.x, 0.11, velocity.z);
            tickTimer = 0;
            return;
        }

        // Simulate jumping out of water (The "bounce")
        Vec3 velocity = mc.player.getDeltaMovement();
        if (tickTimer == 0) {
            mc.player.setDeltaMovement(velocity.x, 0.30, velocity.z);
        } else if (tickTimer == 1) {
            mc.player.setDeltaMovement(velocity.x, 0, velocity.z);
        }

        tickTimer++;
    }

    @Override
    public void onPacketSend(PacketEvent.Send event) {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null) return;

        // Check packet type (MovePlayerPacket)
        if (!(event.packet instanceof ServerboundMovePlayerPacket packet)) return;

        // Check if packet contains a position
        if (!(packet instanceof ServerboundMovePlayerPacket.Pos || packet instanceof ServerboundMovePlayerPacket.PosRot)) {
            return;
        }

        // Conditions to stop bypass: Submerged, falling too fast, or not over liquid
        if (mc.player.isInWater() || mc.player.fallDistance > 3f || !isOverLiquid()) {
            return;
        }

        // If not moving, cancel packet to prevent sliding/desync
        if (mc.player.xxa == 0 && mc.player.zza == 0) {
            event.cancelled = true;
            return;
        }

        // NCP Delay logic
        packetTimer++;
        if (packetTimer < 4) return;
        packetTimer = 0; // Reset timer after success

        event.cancelled = true;

        double x = packet.getX(mc.player.getX());
        double y = packet.getY(mc.player.getY());
        double z = packet.getZ(mc.player.getZ());

        // Offset Y slightly to trick NCP into thinking you are on a block
        if (bypass.isActive() && mc.player.tickCount % 2 == 0) {
            y -= 0.05;
        } else {
            y += 0.05;
        }

        // Create and send the spoofed packet
        Packet<?> newPacket;
        if (packet instanceof ServerboundMovePlayerPacket.Pos) {
            newPacket = new ServerboundMovePlayerPacket.Pos(x, y, z, true, mc.player.horizontalCollision);
        } else {
            newPacket = new ServerboundMovePlayerPacket.PosRot(x, y, z,
                    packet.getYRot(mc.player.getYRot()), packet.getXRot(mc.player.getXRot()),
                    true, mc.player.horizontalCollision);
        }

        mc.player.connection.send(newPacket);
    }

    private boolean isOverLiquid() {
        Minecraft mc = FukntClientClient.mc;
        AABB box = mc.player.getBoundingBox().move(0, -0.5, 0);

        boolean foundLiquid = false;
        boolean foundSolid = false;

        // Check blocks beneath the player's collision box
        for (BlockPos pos : BlockPos.betweenClosed(
                BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {

            Block block = mc.level.getBlockState(pos).getBlock();
            if (block instanceof LiquidBlock) {
                foundLiquid = true;
            } else if (!(block instanceof AirBlock)) {
                foundSolid = true;
            }
        }
        return foundLiquid && !foundSolid;
    }
}