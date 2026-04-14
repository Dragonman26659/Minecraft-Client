package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

import java.util.Comparator;
import java.util.stream.StreamSupport;

@ModuleInfo(name = "CrystalAura", description = "Automatically attacks and places end crystals", category = Category.Combat)
public class CrystalAura extends Module {

    // General Settings
    public final BooleanSetting autoPlace       = new BooleanSetting("Auto Place",     true);
    public final BooleanSetting checkSafety     = new BooleanSetting("Check Safety",   true);
    public final BooleanSetting checkFriend     = new BooleanSetting("Check Friend",   true);
    public final BooleanSetting checkDamage     = new BooleanSetting("Check Damage",   true);

    // Target Settings
    public final BooleanSetting targetPlayers   = new BooleanSetting("Target Players", true);
    public final BooleanSetting targetMobs      = new BooleanSetting("Target Mobs",    false);
    public final BooleanSetting targetAnimals   = new BooleanSetting("Target Animals", false);
    public final BooleanSetting targetFriends   = new BooleanSetting("Target Friends", false);

    public final BooleanSetting autoEquipCrystal = new BooleanSetting("Auto Equip Crystal", true);
    public final BooleanSetting autoEquipSword   = new BooleanSetting("Auto Equip Sword",   true);

    // Values
    public final DoubleSetting damageThreshold  = new DoubleSetting("DThresh",         6.0, 1.0, 20.0, 0.5);
    public final DoubleSetting range            = new DoubleSetting("Break Range",     5.0, 1.0, 10.0, 0.5);
    public final DoubleSetting placeRange       = new DoubleSetting("Place Range",     4.5, 1.0, 10.0, 0.5);

    public CrystalAura() {
        addSettings(autoPlace, targetPlayers, targetMobs, targetAnimals, targetFriends,
                checkSafety, checkFriend, checkDamage, damageThreshold, range, placeRange,
                autoEquipCrystal, autoEquipSword);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean slotChanged = false;

        // 1. Breaking Logic — optionally swap to a sword first
        EndCrystal targetCrystal = getBestCrystal(mc);
        if (targetCrystal != null) {
            if (autoEquipSword.isActive()) {
                int swordSlot = findSwordSlot(mc);
                if (swordSlot != -1 && swordSlot != originalSlot) {
                    mc.player.getInventory().setSelectedSlot(swordSlot);
                    slotChanged = true;
                }
            }

            mc.gameMode.attack(mc.player, targetCrystal);
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (slotChanged) mc.player.getInventory().setSelectedSlot(originalSlot);
            return;
        }

        // 2. Placing Logic — optionally swap to crystals first
        if (autoPlace.isActive()) {
            boolean alreadyHolding = mc.player.getMainHandItem().is(Items.END_CRYSTAL)
                    || mc.player.getOffhandItem().is(Items.END_CRYSTAL);

            if (!alreadyHolding && autoEquipCrystal.isActive()) {
                int crystalSlot = findHotbarSlot(mc, Items.END_CRYSTAL);
                if (crystalSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(crystalSlot);
                    slotChanged = true;
                }
            }

            // Re-check after possible slot change
            if (mc.player.getMainHandItem().is(Items.END_CRYSTAL)
                    || mc.player.getOffhandItem().is(Items.END_CRYSTAL)) {
                doPlace(mc);
            }

            if (slotChanged) mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    private void doPlace(Minecraft mc) {
        BlockPos bestPos = null;
        double maxDamage = 0;

        // Search for Obsidian/Bedrock nearby
        int r = (int) Math.ceil(placeRange.getVal());
        assert mc.player != null;
        BlockPos playerPos = mc.player.blockPosition();

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.offset(x, y, z);

                    if (canPlaceCrystal(mc, pos)) {
                        Vec3 center = Vec3.atBottomCenterOf(pos).add(0, 1, 0);
                        double dist = mc.player.position().distanceTo(center);
                        if (dist > placeRange.getVal()) continue;

                        // Check self safety before considering this spot
                        double selfDmg = calculateExplosionDamage(mc.player, center);
                        if (checkSafety.isActive() && mc.player.getHealth() - selfDmg <= 0) continue;

                        double targetDmg = getMaxDamageOnTargets(mc, center);
                        if (targetDmg > maxDamage) {
                            maxDamage = targetDmg;
                            bestPos = pos;
                        }
                    }
                }
            }
        }

        if (bestPos != null && maxDamage >= (checkDamage.isActive() ? damageThreshold.getVal() : 1.0)) {
            InteractionHand hand = mc.player.getMainHandItem().is(Items.END_CRYSTAL) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            assert mc.gameMode != null;
            mc.gameMode.useItemOn(mc.player, hand, new BlockHitResult(Vec3.atCenterOf(bestPos), Direction.UP, bestPos, false));
        }
    }

    private boolean canPlaceCrystal(Minecraft mc, BlockPos pos) {
        assert mc.level != null;
        var block = mc.level.getBlockState(pos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return false;

        BlockPos air1 = pos.above();
        BlockPos air2 = pos.above(2);

        // Standard placement rules (1.13+ requires air above)
        return mc.level.isEmptyBlock(air1) && mc.level.isEmptyBlock(air2) &&
                mc.level.getEntities(null, new net.minecraft.world.phys.AABB(new Vec3(air1.getX(), air1.getY(), air1.getZ()), new Vec3(air2.above().getX(), air2.above().getY(), air2.above().getZ()))).isEmpty();
    }

    private EndCrystal getBestCrystal(Minecraft mc) {
        assert mc.level != null;
        return StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof EndCrystal)
                .map(e -> (EndCrystal) e)
                .filter(c -> mc.player.distanceTo(c) <= range.getVal())
                .filter(c -> !checkSafety.isActive() || mc.player.getHealth() - calculateExplosionDamage(mc.player, c.position()) > 0)
                .filter(c -> !checkFriend.isActive() || !wouldHurtFriend(mc, c.position()))
                .max(Comparator.comparingDouble(c -> getMaxDamageOnTargets(mc, c.position())))
                .orElse(null);
    }

    private double getMaxDamageOnTargets(Minecraft mc, Vec3 explosionPos) {
        double best = 0;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;

            boolean isFriend = FukntClientClient.fm.isFriend(living.getName().getString());

            // Filtering based on settings
            if (living instanceof Player) {
                if (isFriend && !targetFriends.isActive()) continue;
                if (!isFriend && !targetPlayers.isActive()) continue;
            } else if (living instanceof Monster && !targetMobs.isActive()) {
                continue;
            } else if (living instanceof Animal && !targetAnimals.isActive()) {
                continue;
            }

            double dmg = calculateExplosionDamage(living, explosionPos);
            if (dmg > best) best = dmg;
        }
        return best;
    }

    private double calculateExplosionDamage(LivingEntity entity, Vec3 explosionPos) {
        double dist = entity.position().distanceTo(explosionPos);
        if (dist > 12.0) return 0;

        double rawDamage = (1.0 - dist / 12.0) * 12.0;

        // Armor reduction (simplified)
        double armor = entity.getArmorValue();
        double toughness = (double) entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        double reduction = Math.min(20.0, Math.max(armor / 5.0, armor - rawDamage / (2.0 + toughness / 4.0))) / 25.0;

        return rawDamage * (1.0 - reduction);
    }

    private boolean wouldHurtFriend(Minecraft mc, Vec3 explosionPos) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == mc.player) continue;
            if (!FukntClientClient.fm.isFriend(player.getGameProfile().name())) continue;

            double friendDamage = calculateExplosionDamage(player, explosionPos);
            if (player.getHealth() - friendDamage <= 0) return true;
        }
        return false;
    }

    /**
     * Finds the hotbar slot (0–8) containing the given item, or -1 if not found.
     */
    private int findHotbarSlot(Minecraft mc, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    /**
     * Finds the hotbar slot (0–8) containing any sword, or -1 if not found.
     * Checks for Item subclass rather than a specific item so modded swords work too.
     */
    private int findSwordSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.is(net.minecraft.tags.ItemTags.SWORDS)) return i;
        }
        return -1;
    }
}