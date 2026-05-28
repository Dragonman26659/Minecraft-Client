package plog.charlie;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Places a block at a target position.
 *
 * PlaceResult values communicate back to the GOAP planner:
 *   NEEDS_BLOCK    – requested block type not in inventory; a GET_BLOCK goal was added
 *   NEEDS_CLOSER   – player too far from placement site
 *   PLACING        – actively placing
 *   DONE           – block placed successfully
 *   BLOCKED        – couldn't place (no support, wrong block, etc.)
 *
 * The planner reads {@link #pendingBlockRequest} when result == NEEDS_BLOCK.
 */
public class Placer {

    // ── Config ─────────────────────────────────────────────name─────────────────
    private static final double PLACE_REACH = 4.5;

    // ── Result type ────────────────────────────────────────────────────────────
    public enum PlaceResult { NEEDS_BLOCK, NEEDS_CLOSER, PLACING, DONE, BLOCKED }

    // ── State ──────────────────────────────────────────────────────────────────
    private final Minecraft mc = Minecraft.getInstance();

    private BlockPos placePos       = null;
    private Block    requiredBlock   = null; // null = any block
    private boolean  active         = false;

    /** Set when NEEDS_BLOCK is returned – the planner should fulfil this. */
    public Block pendingBlockRequest = null;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Place any solid block in the hotbar at {@code pos}.
     */
    public void placeAny(BlockPos pos) {
        placePos      = pos;
        requiredBlock = null;
        active        = true;
        pendingBlockRequest = null;
    }

    /**
     * Place a specific {@code block} at {@code pos}.
     * If the block is not in the hotbar, NEEDS_BLOCK is returned and
     * {@link #pendingBlockRequest} is populated for the planner.
     */
    public void placeSpecific(BlockPos pos, Block block) {
        placePos      = pos;
        requiredBlock = block;
        active        = true;
        pendingBlockRequest = null;
    }

    public boolean isActive() { return active; }

    public void stop() { active = false; placePos = null; }

    /**
     * Called every tick by the GOAP planner.
     */
    public PlaceResult tick() {
        if (!active || mc.player == null || mc.level == null) return PlaceResult.BLOCKED;

        // ── Inventory check ────────────────────────────────────────────────────
        int slot = findSlot(requiredBlock);
        if (slot == -1) {
            // No suitable block found
            pendingBlockRequest = requiredBlock; // may be null = "any block"
            return PlaceResult.NEEDS_BLOCK;
        }

        // ── Range check ───────────────────────────────────────────────────────
        Vec3   playerPos = mc.player.position();
        Vec3   placeCenter = Vec3.atCenterOf(placePos);
        double dist = playerPos.distanceTo(placeCenter);
        if (dist > PLACE_REACH) return PlaceResult.NEEDS_CLOSER;

        // ── Already placed? ────────────────────────────────────────────────────
        if (!mc.level.getBlockState(placePos).isAir()) {
            active = false;
            return PlaceResult.DONE;
        }

        // ── Find a support face ────────────────────────────────────────────────
        Direction supportFace = findSupportFace(placePos);
        if (supportFace == null) return PlaceResult.BLOCKED; // nothing to place against

        // ── Look and place ─────────────────────────────────────────────────────
        lookAt(placeCenter);
        mc.player.getInventory().setSelectedSlot(slot);

        BlockPos       supportPos  = placePos.relative(supportFace.getOpposite());
        Vec3           hitVec      = Vec3.atCenterOf(supportPos);
        BlockHitResult hitResult   = new BlockHitResult(hitVec, supportFace, supportPos, false);

        assert mc.gameMode != null;
        mc.gameMode.useItemOn(mc.player, mc.player.getUsedItemHand(), hitResult);

        // Check if it was placed
        if (!mc.level.getBlockState(placePos).isAir()) {
            active = false;
            return PlaceResult.DONE;
        }

        return PlaceResult.PLACING;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * @return hotbar slot index for {@code required}, or first block if null, or -1 if none.
     */
    private int findSlot(Block required) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!(item instanceof BlockItem bi)) continue;
            if (required == null || bi.getBlock() == required) return i;
        }
        return -1;
    }

    /**
     * Returns a direction whose neighbor is a solid block (placement support).
     */
    private Direction findSupportFace(BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos neighbour = pos.relative(d);
            if (!mc.level.getBlockState(neighbour).isAir()) return d;
        }
        return null;
    }

    private void lookAt(Vec3 t) {
        if (mc.player == null) return;
        Vec3 e = mc.player.getEyePosition();
        double dx = t.x - e.x, dy = t.y - e.y, dz = t.z - e.z;
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }
}