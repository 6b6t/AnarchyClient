package net.blockhost.anarchyclient.command;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldCommandsTest {

    @Test
    void locateHelpersFindDeterministicSlimeChunks() {
        assertEquals(12345L, WorldCommands.numericSeed("12345"));
        assertEquals("abc".hashCode(), WorldCommands.numericSeed("abc"));

        BlockPos slimeChunk = WorldCommands.closestSlimeChunk(12345L, 0, 0, 64);

        assertTrue(WorldCommands.isSlimeChunk(12345L,
                Math.floorDiv(slimeChunk.getX(), 16),
                Math.floorDiv(slimeChunk.getZ(), 16)));
    }

    @Test
    void slimeChunksMatchVanillaFarFromOrigin() {
        long seed = -4172144997902289642L;
        Random coords = new Random(42L);
        for (int i = 0; i < 2_000; i++) {
            int chunkX = coords.nextInt(200_001) - 100_000;
            int chunkZ = coords.nextInt(200_001) - 100_000;
            boolean vanilla = WorldgenRandom.seedSlimeChunk(chunkX, chunkZ, seed, 987234911L).nextInt(10) == 0;
            assertEquals(vanilla, WorldCommands.isSlimeChunk(seed, chunkX, chunkZ), chunkX + ", " + chunkZ);
            assertEquals(referenceSlimeChunk(seed, chunkX, chunkZ), vanilla, chunkX + ", " + chunkZ);
        }
    }

    @Test
    void closestSlimeChunkIsTheEuclideanNearest() {
        long seed = 12345L;
        for (int center = -40; center <= 40; center += 8) {
            BlockPos found = WorldCommands.closestSlimeChunk(seed, center, -center, 64);
            int foundDx = Math.floorDiv(found.getX(), 16) - center;
            int foundDz = Math.floorDiv(found.getZ(), 16) + center;
            int foundDistance = foundDx * foundDx + foundDz * foundDz;

            int bestDistance = Integer.MAX_VALUE;
            for (int dx = -20; dx <= 20; dx++) {
                for (int dz = -20; dz <= 20; dz++) {
                    if (WorldCommands.isSlimeChunk(seed, center + dx, -center + dz)) {
                        bestDistance = Math.min(bestDistance, dx * dx + dz * dz);
                    }
                }
            }
            assertEquals(bestDistance, foundDistance, "center " + center);
        }
    }

    /** Independent copy of the vanilla formula, with the same int overflow, using java.util.Random. */
    private static boolean referenceSlimeChunk(final long seed, final int x, final int z) {
        int xx = x * x * 4987142;
        int x1 = x * 5947611;
        long zz = (long) (z * z) * 4392871L;
        int z1 = z * 389711;
        return new Random(seed + xx + x1 + zz + z1 ^ 987234911L).nextInt(10) == 0;
    }
}
