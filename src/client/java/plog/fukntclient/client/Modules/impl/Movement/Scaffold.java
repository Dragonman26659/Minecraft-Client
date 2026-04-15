package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;
import plog.fukntclient.client.settings.impl.DoubleSetting;

import java.util.Arrays;

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath you", category = Category.Movement)
public class Scaffold extends Module {

    public final BooleanSetting tower      = new BooleanSetting("Tower",       false);
    public final BooleanSetting safeWalk   = new BooleanSetting("Safe Walk",   true);
    public final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);
    public final DoubleSetting  delay      = new DoubleSetting("Delay",        0.0, 0.0, 10.0, 1.0);

    private int tickCounter = 0;
    private int savedSlot   = -1;

    public Scaffold() {
        addSettings(tower, safeWalk, autoSwitch, delay);
    }

    @Override
    public void onEnable() {
        tickCounter = 0;
        savedSlot   = -1;
    }

    @Override
    public void onDisable() {
        Minecraft mc = FukntClientClient.mc;
        if (savedSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(savedSlot);
            savedSlot = -1;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (delay.getVal() > 0) {
            tickCounter++;
            if (tickCounter < (int) delay.getVal()) return;
            tickCounter = 0;
        }

        BlockPos belowPlayer = BlockPos.containing(mc.player.position()).below();

        if (!mc.level.getBlockState(belowPlayer).canBeReplaced()) return;

        int newSlot = findBlockSlot(mc, belowPlayer);
        if (newSlot == -1) return;

        int originalSlot = mc.player.getInventory().getSelectedSlot();
        boolean switched = false;

        if (newSlot != originalSlot) {
            if (!autoSwitch.isActive()) return;
            if (savedSlot == -1) savedSlot = originalSlot;
            mc.player.getInventory().setSelectedSlot(newSlot);
            switched = true;
        }

        // Save camera rotation before any placement rotation happens
        float savedYaw   = mc.player.getYRot();
        float savedPitch = mc.player.getXRot();

        scaffoldTo(mc, belowPlayer);

        // Restore camera to where the player was looking
        mc.player.setYRot(savedYaw);
        mc.player.setXRot(savedPitch);

        if (tower.isActive() && mc.player.onGround()) {
            mc.player.jumpFromGround();
        }

        if (switched) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            savedSlot = -1;
        }
    }

    private void scaffoldTo(Minecraft mc, BlockPos belowPlayer) {
        if (placeBlock(mc, belowPlayer)) return;

        Direction[] sides = Direction.values();

        for (Direction side : sides) {
            if (placeBlock(mc, belowPlayer.relative(side))) return;
        }

        for (Direction side : sides) {
            for (Direction side2 : Arrays.copyOfRange(sides, side.ordinal(), 6)) {
                if (side.getOpposite().equals(side2)) continue;
                if (placeBlock(mc, belowPlayer.relative(side).relative(side2))) return;
            }
        }
    }

    private boolean placeBlock(Minecraft mc, BlockPos pos) {
        Vec3 eyesPos = mc.player.getEyePosition();

        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            Direction side2 = side.getOpposite();

            if (eyesPos.distanceToSqr(Vec3.atCenterOf(pos)) >=
                    eyesPos.distanceToSqr(Vec3.atCenterOf(neighbor)))
                continue;

            if (!canBeClicked(mc, neighbor)) continue;

            Vec3 hitVec = Vec3.atCenterOf(neighbor)
                    .add(Vec3.atLowerCornerOf(side2.getUnitVec3i()).scale(0.5));

            if (eyesPos.distanceToSqr(hitVec) > 18.0625) continue;

            rotateToward(mc, hitVec);
            mc.gameMode.useItemOn(
                    mc.player,
                    InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(hitVec, side2, neighbor, false)
            );
            mc.player.swing(InteractionHand.MAIN_HAND);

            return true;
        }

        return false;
    }

    private int findBlockSlot(Minecraft mc, BlockPos belowPlayer) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;

            Block block = Block.byItem(stack.getItem());
            BlockState state = block.defaultBlockState();

            if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                continue;

            if (block instanceof FallingBlock &&
                    FallingBlock.isFree(mc.level.getBlockState(belowPlayer.below())))
                continue;

            return i;
        }
        return -1;
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