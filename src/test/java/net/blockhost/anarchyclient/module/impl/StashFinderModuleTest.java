package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

class StashFinderModuleTest {

    @Test
    void findsAStashThatCrossesAChunkBorder() {
        Map<Long, Integer> counts = Map.of(ChunkPos.pack(-1, 0), 5, ChunkPos.pack(0, 0), 5);

        List<StashFinderModule.Stash> stashes = StashFinderModule.findStashes(counts, 8, new HashSet<>());

        assertEquals(1, stashes.size());
        assertEquals(10, stashes.getFirst().containers());
    }

    @Test
    void reportsEachStashOnce() {
        Map<Long, Integer> counts = Map.of(ChunkPos.pack(0, 0), 9);
        Set<Long> reported = new HashSet<>();

        assertEquals(1, StashFinderModule.findStashes(counts, 8, reported).size());
        assertTrue(StashFinderModule.findStashes(counts, 8, reported).isEmpty());
    }

    @Test
    void ignoresScatteredContainers() {
        Map<Long, Integer> counts = Map.of(ChunkPos.pack(0, 0), 4, ChunkPos.pack(5, 5), 4);

        assertTrue(StashFinderModule.findStashes(counts, 8, new HashSet<>()).isEmpty());
    }
}
