package net.blockhost.anarchyclient.command;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

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
}
