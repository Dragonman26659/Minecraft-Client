package plog.fukntclient.client.Managers;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

public class TargetManager {
    @Getter @Setter
    private LocationData target = null;

    public boolean hasTarget() {
        return target != null;
    }

    /**
     * Returns the target BlockPos scaled to the player's current dimension.
     */
    public BlockPos getScaledPos(String currentDimension) {
        if (target == null) return null;

        int x = target.getX();
        int y = target.getY();
        int z = target.getZ();
        String targetDim = target.getDimension();

        // No conversion needed
        if (targetDim.equals(currentDimension)) {
            return new BlockPos(x, y, z);
        }

        // Target is Overworld, Player is Nether
        if (targetDim.equals("minecraft:overworld") && currentDimension.equals("minecraft:the_nether")) {
            return new BlockPos(x / 8, y, z / 8);
        }

        // Target is Nether, Player is Overworld
        if (targetDim.equals("minecraft:the_nether") && currentDimension.equals("minecraft:overworld")) {
            return new BlockPos(x * 8, y, z * 8);
        }

        // Default fallback (e.g., End)
        return new BlockPos(x, y, z);
    }
}