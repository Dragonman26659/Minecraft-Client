package plog.fukntclient.client.Modules.impl.Movement;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import plog.fukntclient.client.FukntClientClient;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Modules.Module;
import plog.fukntclient.client.Modules.ModuleInfo;
import plog.fukntclient.client.settings.impl.BooleanSetting;

@ModuleInfo(name = "Parkour", description = "Auto-jumps at block edges", category = Category.Movement)
public class Parkour extends Module {

    public final BooleanSetting requireSprint = new BooleanSetting("Require Sprint", true);
    public final BooleanSetting onlyForward   = new BooleanSetting("Only Forward",   true);

    public Parkour() {
        addSettings(requireSprint, onlyForward);
    }

    @Override
    public void onTick() {
        Minecraft mc = FukntClientClient.mc;
        if (mc.player == null || mc.level == null) return;
        if (!mc.player.onGround())   return;
        if (mc.player.isInWater())   return;
        if (mc.player.isInLava())    return;
        if (mc.player.isJumping())       return;

        if (requireSprint.isActive() && !mc.player.isSprinting()) return;
        if (onlyForward.isActive()   && !mc.options.keyUp.isDown()) return;

        // Check if the block directly in front-below the player is air
        BlockPos below = mc.player.blockPosition().below();
        boolean edgeAhead = false;

        for (Direction dir : new Direction[]{ Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
            BlockPos neighbour = below.relative(dir);
            if (mc.level.getBlockState(neighbour).isAir()) {
                edgeAhead = true;
                break;
            }
        }

        if (edgeAhead) {
            mc.player.jumpFromGround();
        }
    }
}