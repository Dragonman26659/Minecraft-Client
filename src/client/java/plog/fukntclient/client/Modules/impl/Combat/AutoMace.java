package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

@ModuleInfo(name = "AutoMace", description = "Attacks targets while falling with a mace", category = Category.Combat)
public class AutoMace extends Module {

    public final BooleanSetting attackAnimals = new BooleanSetting("Attack Animals", false);
    public final BooleanSetting attackMobs    = new BooleanSetting("Attack Mobs",    true);
    public final BooleanSetting attackPlayers = new BooleanSetting("Attack Players", true);
    public final BooleanSetting attackFriends = new BooleanSetting("Attack Friends", false);
    public final BooleanSetting autoEquip     = new BooleanSetting("Auto Equip",     true);
    public final BooleanSetting requireMace   = new BooleanSetting("Require Mace",   true);

    public final DoubleSetting attackRange    = new DoubleSetting("Attack Range", 5.0, 1.0, 10.0, 0.1);
    public final DoubleSetting minDamage      = new DoubleSetting("Min Damage", 10.0, 5.0, 100.0, 0.5);

    public AutoMace() {
        addSettings(attackAnimals, attackMobs, attackPlayers, attackFriends, autoEquip, requireMace, attackRange, minDamage);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        // Only act while falling
        if (mc.player.getDeltaMovement().y >= 0) return;

        // Calculate expected damage based on vanilla 1.21 mace mechanics
        double expectedDamage = getEstimatedDamage((float)mc.player.fallDistance);
        if (expectedDamage < minDamage.getVal()) return;

        // Check for targets BEFORE doing any auto-equip logic
        Entity target = getBestTarget(mc);
        if (target == null) return;

        // --- AutoEquip Logic ---
        boolean holdingMace = mc.player.getMainHandItem().is(Items.MACE) || mc.player.getOffhandItem().is(Items.MACE);

        // "Falling fast" criteria: Y velocity is rapid, or we've crossed the threshold for a smash attack (1.5 blocks)
        boolean fallingFast = mc.player.getDeltaMovement().y < -0.5 || mc.player.fallDistance > 1.5f;

        if (!holdingMace && autoEquip.isActive() && fallingFast) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).is(Items.MACE)) {
                    mc.player.getInventory().setSelectedSlot(i);
                    holdingMace = true;
                    break;
                }
            }
        }

        if (requireMace.isActive() && !holdingMace) return;

        // Ensure we respect attack cool down for maximum Mace damage
        if (mc.player.getAttackStrengthScale(0.0f) >= 1.0f) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private Entity getBestTarget(Minecraft mc) {
        Entity best = null;
        int bestPriority = -1;

        assert mc.level != null;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;

            assert mc.player != null;
            double dist = mc.player.distanceTo(entity);
            if (dist > attackRange.getVal()) continue;

            int priority = getPriority(entity);
            if (priority == -1) continue;

            if (priority > bestPriority) {
                bestPriority = priority;
                best = entity;
            }
        }
        return best;
    }

    private int getPriority(Entity entity) {
        if (isFriend(entity) && !attackFriends.isActive()) return -1;
        if (entity instanceof Player)  return attackPlayers.isActive() ? 3 : -1;
        if (entity instanceof Monster) return attackMobs.isActive()    ? 2 : -1;
        if (entity instanceof Animal)  return attackAnimals.isActive() ? 1 : -1;
        return -1;
    }

    private boolean isFriend(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        return FukntClientClient.fm.isFriend(player.getGameProfile().name());
    }

    /**
     * Approximates Minecraft 1.21 Mace fall damage calculations.
     * Requires falling > 1.5 blocks to trigger the smash attack multipliers.
     */
    private double getEstimatedDamage(float fallDistance) {
        double baseDamage = 5.0;

        // Smash attack does not trigger if fall distance is 1.5 blocks or fewer
        if (fallDistance <= 1.5f) {
            return baseDamage;
        }

        double bonus;
        // First 3 blocks fallen: +3 damage per block
        if (fallDistance <= 3.0f) {
            bonus = fallDistance * 3.0;
        }
        // Next 5 blocks (up to 8): +1.5 damage per block
        else if (fallDistance <= 8.0f) {
            bonus = (3.0 * 3.0) + ((fallDistance - 3.0) * 1.5);
        }
        // Anything past 8 blocks: +1 damage per block
        else {
            bonus = (3.0 * 3.0) + (5.0 * 1.5) + ((fallDistance - 8.0));
        }

        return baseDamage + bonus;
    }
}