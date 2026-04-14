package plog.fukntclient.client.Util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;

public class TeleportUtil {

    private static final Minecraft mc = FukntClientClient.mc;

    // Send position packets without actually moving the client
    public static void sendPosition(double x, double y, double z) {
        PacketUtil.send(new ServerboundMovePlayerPacket.Pos(x, y, z, mc.player.onGround(), false));
    }

    public static void sendPosition(Vec3 pos) {
        sendPosition(pos.x, pos.y, pos.z);
    }

    // Teleport client visually and via packet
    public static void teleport(double x, double y, double z) {
        //mc.player.move(x, y, z);
        sendPosition(x, y, z);
    }

    public static void teleport(Vec3 pos) {
        teleport(pos.x, pos.y, pos.z);
    }

    // Save current position, run action, return — all in one tick via packets only
    // Client doesn't visually move, server thinks you moved
    public static void packetTeleportAndBack(Vec3 target, Runnable action) {
        Vec3 origin = mc.player.position();

        sendPosition(target);  // tell server we're at target
        action.run();          // do the thing (attack etc)
        sendPosition(origin);  // tell server we're back
    }

    // Interpolate towards a target over multiple ticks — smoother for things like phase
    public static Vec3 towards(Vec3 from, Vec3 to, double speed) {
        Vec3 diff = to.subtract(from);
        double dist = diff.length();
        if (dist <= speed) return to;
        return from.add(diff.normalize().scale(speed));
    }

    // Check if a position is safe to stand on
    public static boolean isSafe(Vec3 pos) {
        if (mc.level == null) return false;
        net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(pos);
        return mc.level.getBlockState(bp).isAir()
                && mc.level.getBlockState(bp.above()).isAir()
                && !mc.level.getBlockState(bp.below()).isAir();
    }
}