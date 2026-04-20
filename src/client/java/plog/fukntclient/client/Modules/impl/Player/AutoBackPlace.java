package plog.fukntclient.client.Modules.impl.Player;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;

import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "AutoBackPlace", description = "Fills the tunnel behind you while mining", category = Category.Player)
public class AutoBackPlace extends Module {

    private final BooleanSetting onlyWhileMining = new BooleanSetting("Only While Mining", true);
    private final BooleanSetting restoreRotation = new BooleanSetting("Restore Rotation",  true);

    // Tracks what block was originally at each position before it was mined
    private final Map<BlockPos, Block> blockMemory = new HashMap<>();

    private BlockPos lastPos = null;

    public AutoBackPlace() {
        addSettings(onlyWhileMining, restoreRotation);
    }

    @Override
    public void onEnable() {
        blockMemory.clear();
        lastPos = null;
    }

    @Override
    public void onDisable() {
        blockMemory.clear();
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        BlockPos currentPos = mc.player.blockPosition();

        // Record what blocks exist around the player before they get mined
        recordNearbyBlocks(mc, currentPos);

        if (lastPos != null && !lastPos.equals(currentPos)) {
            if (onlyWhileMining.isActive() && !mc.options.keyAttack.isDown()) {
                lastPos = currentPos;
                return;
            }

            // Try to fill the block the player just walked out of
            if (mc.level.getBlockState(lastPos).isAir()) {
                tryFill(mc, lastPos);
            }
        }

        lastPos = currentPos;
    }

    /**
     * Snapshot blocks around the player so we know what was there before mining.
     */
    private void recordNearbyBlocks(Minecraft mc, BlockPos center) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState state = mc.level.getBlockState(p);
                    if (!state.isAir() && !blockMemory.containsKey(p)) {
                        blockMemory.put(p, state.getBlock());
                    }
                }
            }
        }
    }

    private void tryFill(Minecraft mc, BlockPos pos) {
        // Find the best slot: prefer the original block, fall back to any solid block
        Block wanted = blockMemory.get(pos);
        int slot = findBestSlot(mc, wanted);
        if (slot == -1) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched  = slot != originalSlot;

        // Switch via packet so the server knows
        if (switched) {
            mc.player.getInventory().setSelectedSlot(slot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
        }

        float savedYaw   = mc.player.getYRot();
        float savedPitch = mc.player.getXRot();

        boolean placed = placeBlock(mc, pos);

        if (restoreRotation.isActive()) {
            mc.player.setYRot(savedYaw);
            mc.player.setXRot(savedPitch);
        }

        if (placed) {
            blockMemory.remove(pos); // Consumed — don't try again
        }

        if (switched) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
        }
    }

    /**
     * Mirrors Scaffold's placeBlock — finds a solid adjacent face and sends a
     * proper useItemOn to the server.
     */
    private boolean placeBlock(Minecraft mc, BlockPos pos) {
        Vec3 eyesPos = mc.player.getEyePosition();

        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            Direction placeFace = side.getOpposite();

            // Skip neighbors that are further from eyes than the target itself
            if (eyesPos.distanceToSqr(Vec3.atCenterOf(pos)) >=
                    eyesPos.distanceToSqr(Vec3.atCenterOf(neighbor)))
                continue;

            // Neighbor must be a solid, non-replaceable block to click against
            if (!canBeClicked(mc, neighbor)) continue;

            Vec3 hitVec = Vec3.atCenterOf(neighbor)
                    .add(Vec3.atLowerCornerOf(placeFace.getUnitVec3i()).scale(0.5));

            // 4.25 block reach check (4.25² = 18.0625)
            if (eyesPos.distanceToSqr(hitVec) > 18.0625) continue;

            rotateToward(mc, hitVec);

            mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(hitVec, placeFace, neighbor, false)
            );
            mc.player.swing(InteractionHand.MAIN_HAND);

            return true;
        }

        return false;
    }

    /**
     * Returns the hotbar slot with the best matching block.
     * Priority: exact original block > any full solid block.
     */
    private int findBestSlot(Minecraft mc, Block wanted) {
        int fallback = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem bi)) continue;

            Block block = bi.getBlock();

            // Exact match — use immediately
            if (wanted != null && block == wanted) return i;

            // Otherwise keep as fallback if it's a full solid cube
            if (fallback == -1) {
                BlockState state = block.defaultBlockState();
                if (state.isCollisionShapeFullBlock(
                        mc.level, mc.player.blockPosition())) {
                    fallback = i;
                }
            }
        }

        return fallback;
    }

    private boolean canBeClicked(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    private void rotateToward(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }
}