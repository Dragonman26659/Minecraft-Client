package plog.fukntclient.client.Managers;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public class SeedManager {
    public static long currentSeed = 0;
    public static boolean isSeedSet = false;

    // In a real implementation, you would initialize a ChunkGenerator here
    // using the seed to simulate the world.
    public static void setSeed(long seed) {
        currentSeed = seed;
        isSeedSet = true;
    }
}