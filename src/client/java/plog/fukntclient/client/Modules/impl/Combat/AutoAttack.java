package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "AutoAttack", description = "Attacks the entity you are looking at", category = Category.Combat)
public class AutoAttack extends Module {

    public final BooleanSetting attackAnimals  = new BooleanSetting("Attack Animals",  false);
    public final BooleanSetting attackMobs     = new BooleanSetting("Attack Mobs",     true);
    public final BooleanSetting attackPlayers  = new BooleanSetting("Attack Players",  false);
    public final BooleanSetting attackFriends  = new BooleanSetting("Attack Friends",  false);

    public final DoubleSetting  attackRange    = new DoubleSetting("Attack Range", 4.0, 1.0, 6.0, 0.1);

    public AutoAttack() {
        addSettings(attackAnimals, attackMobs, attackPlayers, attackFriends, attackRange);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        // Check if the attack is fully charged
        if (mc.player.getAttackStrengthScale(0.0f) < 1.0) return;

        double range = attackRange.getVal();

        // 1. Define the raycast parameters
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookVec = mc.player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

        // Create a bounding box that covers the area of the raycast
        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(lookVec.scale(range))
                .inflate(1.0D, 1.0D, 1.0D);

        // 2. Perform the raycast to find the entity under the crosshair
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                mc.player,
                eyePos,
                reachVec,
                searchBox,
                this::isValidTarget,
                range * range
        );

        // 3. If we hit a valid entity, attack it
        if (hitResult != null && hitResult.getEntity() != null) {
            Entity target = hitResult.getEntity();

            mc.gameMode.attack(mc.player, target);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private boolean isValidTarget(Entity entity) {
        if (!entity.isAlive() || entity == FukntClientClient.mc.player) return false;

        // Friends check
        if (isFriend(entity) && !attackFriends.isActive()) return false;

        if (entity instanceof Monster)        return attackMobs.isActive();
        if (entity instanceof Animal)         return attackAnimals.isActive();
        if (entity instanceof Player)         return attackPlayers.isActive();

        return false;
    }

    private boolean isFriend(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        return FukntClientClient.fm.isFriend(player.getGameProfile().name());
    }
}