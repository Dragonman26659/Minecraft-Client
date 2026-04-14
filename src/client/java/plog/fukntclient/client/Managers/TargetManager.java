package plog.fukntclient.client.Managers;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;

public class TargetManager {
    @Getter @Setter
    private BlockPos targetPos = null;

    public boolean hasTarget() {
        return targetPos != null;
    }
}