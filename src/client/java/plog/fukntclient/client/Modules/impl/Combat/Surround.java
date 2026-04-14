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
import net.minecraft.world.level.block.Blocks;
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

    // --- Target Settings (mirroring CrystalAura) ---
    public final BooleanSetting targetPlayers  = new BooleanSetting("Target Players",  true);
    public final BooleanSetting targetMobs     = new BooleanSetting("Target Mobs",     false);
    public final BooleanSetting targetAnimals  = new BooleanSetting("Target Animals",  false);
    public final BooleanSetting targetFriends  = new BooleanSetting("Target Friends",  false);
    public final BooleanSetting targetSelf     = new BooleanSetting("Target Self",     false);

    // --- Block Settings ---
    public final ModeSetting   pattern        = new ModeSetting("Pattern", "Cardinal", "Full", "Feet");
    public final BooleanSetting autoEquip     = new BooleanSetting("Auto Equip",      true);
    public final BooleanSetting rotate        = new BooleanSetting("Rotate",          false);

    // --- Range / Timing ---
    public final DoubleSetting placeRange     = new DoubleSetting("Place Range",  4.5, 1.0, 8.0, 0.5);
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

        // Make sure we have a solid block to place
        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean slotChanged = false;

        boolean holdingBlock = isHoldingPlaceableBlock(mc);
        if (!holdingBlock && autoEquip.isActive()) {
            int blockSlot = findSolidBlockSlot(mc);
            if (blockSlot == -1) return; // No blocks available
            mc.player.getInventory().setSelectedSlot(blockSlot);
            slotChanged = true;
        } else if (!holdingBlock) {
            return;
        }

        List<BlockPos> positions = getSurroundPositions(target);

        for (BlockPos pos : positions) {
            if (!mc.level.isEmptyBlock(pos)) continue; // Already filled

            // Distance check from the player
            Vec3 center = Vec3.atCenterOf(pos);
            if (mc.player.position().distanceTo(center) > placeRange.getVal()) continue;

            // Find an adjacent solid face to place against
            Direction face = getAdjacentFace(mc, pos);
            if (face == null) continue;

            BlockPos support = pos.relative(face.getOpposite());
            Vec3 hitVec = Vec3.atCenterOf(support).relative(face, 0.5);

            if (rotate.isActive()) {
                faceBlock(mc, hitVec);
            }

            InteractionHand hand = mc.player.getMainHandItem().getItem() instanceof BlockItem
                    ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND;

            mc.gameMode.useItemOn(
                    mc.player, hand,
                    new BlockHitResult(hitVec, face, support, false)
            );
            mc.player.swing(hand);
        }

        if (slotChanged) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
    }

    // -------------------------------------------------------------------------
    // Surround pattern generators
    // -------------------------------------------------------------------------

    /**
     * Returns a list of {@link BlockPos} that should be filled depending on the
     * selected pattern, centred on the target's block position.
     *
     * <ul>
     *   <li><b>Cardinal</b> – 4 horizontal blocks at foot level only.</li>
     *   <li><b>Full</b>     – 4 horizontal + 4 diagonal at foot level (8 total).</li>
     *   <li><b>Feet</b>     – Cardinal at foot level AND one block above (traps legs).</li>
     * </ul>
     */
    private List<BlockPos> getSurroundPositions(LivingEntity target) {
        BlockPos feet = target.blockPosition(); // Y = foot block
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
                // Two layers: foot level and knee level
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
    // Target selection — mirrors CrystalAura logic
    // -------------------------------------------------------------------------

    private LivingEntity findTarget(Minecraft mc) {
        // Self-surround shortcut
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
                continue; // Unknown entity type — skip
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

    /**
     * Finds a horizontal or vertical adjacent face of {@code pos} that is
     * backed by a solid block the player can place against.
     */
    private Direction findAdjacentSolidFace(Minecraft mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            if (!mc.level.isEmptyBlock(neighbour)) {
                return dir.getOpposite(); // The face on the target pos side
            }
        }
        return null;
    }

    private Direction getAdjacentFace(Minecraft mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            if (!mc.level.isEmptyBlock(neighbour)) {
                return dir; // Face of the neighbouring solid block facing our empty pos
            }
        }
        return null;
    }

    private boolean isHoldingPlaceableBlock(Minecraft mc) {
        return mc.player.getMainHandItem().getItem() instanceof BlockItem
                || mc.player.getOffhandItem().getItem() instanceof BlockItem;
    }

    /**
     * Finds a hotbar slot with a solid, non-interactive block (obsidian preferred,
     * falling back to any generic solid block item).
     */
    private int findSolidBlockSlot(Minecraft mc) {
        // Prefer obsidian for griefing resistance
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.OBSIDIAN)) return i;
        }
        // Fall back to any placeable block
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                // Exclude common non-solid/interactive blocks
                var block = bi.getBlock();
                if (block != Blocks.CHEST && block != Blocks.CRAFTING_TABLE
                        && block != Blocks.FURNACE && block != Blocks.TNT) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Rotates the player's yaw/pitch toward the given world position so that
     * server-side placement validation passes on strict servers.
     */
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