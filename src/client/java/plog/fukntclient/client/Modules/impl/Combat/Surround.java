package plog.fukntclient.client.Modules.impl.Combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;
import plog.fukntclient.client.settings.impl.ModeSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(name = "Surround", description = "Surrounds a target entity with blocks", category = Category.Combat)
public class Surround extends Module {

    public final BooleanSetting targetPlayers = new BooleanSetting("Target Players", true);
    public final BooleanSetting targetMobs    = new BooleanSetting("Target Mobs",    false);
    public final BooleanSetting targetAnimals = new BooleanSetting("Target Animals", false);
    public final BooleanSetting targetFriends = new BooleanSetting("Target Friends", false);
    public final BooleanSetting targetSelf    = new BooleanSetting("Target Self",    false);

    public final ModeSetting    pattern       = new ModeSetting("Pattern", "Cardinal", "Full", "Feet");
    public final BooleanSetting autoEquip     = new BooleanSetting("Auto Equip", true);
    public final BooleanSetting rotate        = new BooleanSetting("Rotate",     false);

    public final DoubleSetting placeRange     = new DoubleSetting("Place Range",  4.5, 1.0, 8.0,  0.5);
    public final DoubleSetting targetRange    = new DoubleSetting("Target Range", 6.0, 1.0, 16.0, 0.5);

    public Surround() {
        addSettings(
                targetPlayers, targetMobs, targetAnimals, targetFriends, targetSelf,
                pattern, autoEquip, rotate,
                placeRange, targetRange
        );
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        LivingEntity target = findTarget(mc);
        if (target == null) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean slotChanged = false;

        boolean holdingBlock = isHoldingPlaceableBlock(mc);
        if (!holdingBlock && autoEquip.isActive()) {
            int blockSlot = findSolidBlockSlot(mc);
            if (blockSlot == -1) return;
            mc.player.getInventory().setSelectedSlot(blockSlot);
            slotChanged = true;
        } else if (!holdingBlock) {
            return;
        }

        List<BlockPos> positions = getSurroundPositions(target);
        Vec3 eyesPos = mc.player.getEyePosition();

        // Save camera rotation before the placement loop — only matters if rotate is on,
        // but cheapest to always save and restore unconditionally
        float savedYaw   = mc.player.getYRot();
        float savedPitch = mc.player.getXRot();

        for (BlockPos pos : positions) {
            if (!mc.level.isEmptyBlock(pos)) continue;

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.relative(dir);

                if (!canBeClicked(mc, neighbor)) continue;

                Direction faceOnNeighbor = dir.getOpposite();

                Vec3 hitVec = Vec3.atCenterOf(neighbor)
                        .add(Vec3.atLowerCornerOf(dir.getUnitVec3i()).scale(0.5));

                if (eyesPos.distanceToSqr(Vec3.atCenterOf(pos)) >=
                        eyesPos.distanceToSqr(Vec3.atCenterOf(neighbor)))
                    continue;

                double rangeSq = Math.min(placeRange.getVal() * placeRange.getVal(), 18.0625);
                if (eyesPos.distanceToSqr(hitVec) > rangeSq) continue;

                if (rotate.isActive()) faceBlock(mc, hitVec);

                InteractionHand hand = mc.player.getMainHandItem().getItem() instanceof BlockItem
                        ? InteractionHand.MAIN_HAND
                        : InteractionHand.OFF_HAND;

                mc.gameMode.useItemOn(
                        mc.player, hand,
                        new BlockHitResult(hitVec, faceOnNeighbor, neighbor, false)
                );
                mc.player.swing(hand);
                break;
            }
        }

        // Restore camera to where the player was looking before any placements
        mc.player.setYRot(savedYaw);
        mc.player.setXRot(savedPitch);

        if (slotChanged) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    // -------------------------------------------------------------------------
    // Surround pattern generators
    // -------------------------------------------------------------------------

    private List<BlockPos> getSurroundPositions(LivingEntity target) {
        BlockPos feet = target.blockPosition();
        List<BlockPos> positions = new ArrayList<>();

        switch (pattern.getCurrentMode()) {
            case "Cardinal" -> {
                positions.add(feet.north());
                positions.add(feet.south());
                positions.add(feet.east());
                positions.add(feet.west());
            }
            case "Full" -> {
                positions.add(feet.north());
                positions.add(feet.south());
                positions.add(feet.east());
                positions.add(feet.west());
                positions.add(feet.north().east());
                positions.add(feet.north().west());
                positions.add(feet.south().east());
                positions.add(feet.south().west());
            }
            case "Feet" -> {
                for (BlockPos base : List.of(feet, feet.above())) {
                    positions.add(base.north());
                    positions.add(base.south());
                    positions.add(base.east());
                    positions.add(base.west());
                }
            }
        }

        return positions;
    }

    // -------------------------------------------------------------------------
    // Target selection
    // -------------------------------------------------------------------------

    private LivingEntity findTarget(Minecraft mc) {
        if (targetSelf.isActive()) return mc.player;

        LivingEntity best = null;
        double closestDist = Double.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (living == mc.player) continue;
            if (!living.isAlive()) continue;

            boolean isFriend = FukntClientClient.fm.isFriend(living.getName().getString());

            if (living instanceof Player) {
                if (isFriend && !targetFriends.isActive()) continue;
                if (!isFriend && !targetPlayers.isActive()) continue;
            } else if (living instanceof Monster) {
                if (!targetMobs.isActive()) continue;
            } else if (living instanceof Animal) {
                if (!targetAnimals.isActive()) continue;
            } else {
                continue;
            }

            double dist = mc.player.distanceTo(living);
            if (dist > targetRange.getVal()) continue;

            if (dist < closestDist) {
                closestDist = dist;
                best = living;
            }
        }

        return best;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean canBeClicked(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private boolean isHoldingPlaceableBlock(Minecraft mc) {
        return mc.player.getMainHandItem().getItem() instanceof BlockItem
                || mc.player.getOffhandItem().getItem() instanceof BlockItem;
    }

    private int findSolidBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) return i;
        }
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) continue;

            Block block = bi.getBlock();
            BlockState state = block.defaultBlockState();

            if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                continue;

            if (block == Blocks.CHEST || block == Blocks.CRAFTING_TABLE
                    || block == Blocks.FURNACE || block == Blocks.TNT)
                continue;

            return i;
        }
        return -1;
    }

    private void faceBlock(Minecraft mc, Vec3 target) {
        Vec3 eyePos = mc.player.getEyePosition(1.0f);
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }
}