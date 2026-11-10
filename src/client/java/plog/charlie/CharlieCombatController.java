package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.stream.StreamSupport;

/** Low-latency combat loop kept separate from survival GOAP replanning. */
public final class CharlieCombatController {
    private final Minecraft mc = Minecraft.getInstance();
    private boolean enabled;
    private String requestedTarget = "";
    private int ticksSinceTarget;

    public void start(String targetName) { requestedTarget = targetName == null ? "" : targetName; enabled = true; ticksSinceTarget = 0; }
    public void stop() { enabled = false; requestedTarget = ""; }
    public boolean isEnabled() { return enabled; }

    public void tick() {
        if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;
        Player target = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(p -> p != mc.player && p.isAlive())
                .filter(p -> requestedTarget.isBlank() || p.getGameProfile().name().equalsIgnoreCase(requestedTarget))
                .min(Comparator.comparingDouble(mc.player::distanceToSqr)).orElse(null);
        if (target == null) {
            if (++ticksSinceTarget > 100) stop();
            return;
        }
        ticksSinceTarget = 0;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYRot((float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
        mc.player.setXRot((float)-Math.toDegrees(Math.atan2(dy, horizontal)));
        if (mc.player.distanceToSqr(target) <= 3.5 * 3.5 && mc.player.getAttackStrengthScale(0.0f) >= 0.92f) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}
