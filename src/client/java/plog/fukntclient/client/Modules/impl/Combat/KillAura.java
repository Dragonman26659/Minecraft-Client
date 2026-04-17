package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

import java.util.List;
import java.util.stream.StreamSupport;

@ModuleInfo(name = "KillAura", description = "Automatically attacks entities in a radius around you", category = Category.Combat)
public class KillAura extends Module {

    public final BooleanSetting attackAnimals  = new BooleanSetting("Attack Animals",  false);
    public final BooleanSetting attackMobs     = new BooleanSetting("Attack Mobs",     true);
    public final BooleanSetting attackPlayers  = new BooleanSetting("Attack Players",  false);
    public final BooleanSetting attackEnemies = new BooleanSetting("Attack Enemies", true);
    public final BooleanSetting attackFriends  = new BooleanSetting("Attack Friends",  false);
    public final BooleanSetting multiTarget    = new BooleanSetting("Multi Target",    false);

    public final DoubleSetting  attackRange    = new DoubleSetting("Attack Range", 4.0, 1.0, 6.0, 0.1);

    public KillAura() {
        addSettings(attackAnimals, attackMobs, attackPlayers, attackFriends, attackEnemies, multiTarget, attackRange);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;

        // Ensure we only attack when the weapon is ready for full damage
        if (mc.player.getAttackStrengthScale(0.0f) < 1.0) return;

        double rangeSq = attackRange.getVal() * attackRange.getVal();

        // Find all valid targets within range
        List<Entity> targets = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(entity -> entity != mc.player && entity.isAlive())
                .filter(this::isValidTarget)
                .filter(entity -> mc.player.distanceToSqr(entity) <= rangeSq)
                .sorted((e1, e2) -> {
                    boolean isE1Enemy = isEnemy(e1);
                    boolean isE2Enemy = isEnemy(e2);
                    if (isE1Enemy && !isE2Enemy) return -1;
                    if (!isE1Enemy && isE2Enemy) return 1;
                    return Double.compare(mc.player.distanceToSqr(e1), mc.player.distanceToSqr(e2));
                })
                .toList();

        if (targets.isEmpty()) return;

        if (multiTarget.isActive()) {
            // Attack everyone in the list (Be careful, this looks very bot-like)
            for (Entity target : targets) {
                attack(target);
            }
        } else {
            // Focus only on the closest target
            attack(targets.getFirst());
        }
    }

    private void attack(Entity target) {
        Minecraft mc = FukntClientClient.mc;
        if (mc.gameMode == null || mc.player == null) return;

        // Perform the attack
        mc.gameMode.attack(mc.player, target);
        mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    private boolean isValidTarget(Entity entity) {
        if (isFriend(entity) && !attackFriends.isActive()) return false;
        if (isEnemy(entity)) return attackEnemies.isActive(); // Priority check

        if (entity instanceof Monster) return attackMobs.isActive();
        if (entity instanceof Animal)  return attackAnimals.isActive();
        if (entity instanceof Player)  return attackPlayers.isActive();
        return false;
    }

    private boolean isEnemy(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        return FukntClientClient.fm.isEnemy(player.getGameProfile().name());
    }

    private boolean isFriend(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        return FukntClientClient.fm.isFriend(player.getGameProfile().name());
    }
}