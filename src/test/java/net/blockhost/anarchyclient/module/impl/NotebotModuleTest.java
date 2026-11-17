package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.impl.NotebotModule.NoteBlockPlan;
import net.blockhost.anarchyclient.module.impl.NotebotModule.ScannedNoteBlock;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebotModuleTest {

    private static final BlockPos NEAR = new BlockPos(1, 0, 0);
    private static final BlockPos FAR = new BlockPos(3, 0, 0);

    @Test
    void assignsTheBlockThatNeedsTheFewestTuningClicks() {
        NoteBlockPlan plan = NoteBlockPlan.create(NotebotModule.parseSong("6"), List.of(
                new ScannedNoteBlock(NEAR, 5, 1.0),
                new ScannedNoteBlock(FAR, 6, 9.0)));

        assertTrue(plan.possible());
        assertEquals(Map.of(6, List.of(FAR)), plan.blocksByNote());
        assertTrue(plan.tuneHits().isEmpty());
    }

    @Test
    void prefersTheNearerBlockWhenTuningCostIsEqual() {
        NoteBlockPlan plan = NoteBlockPlan.create(NotebotModule.parseSong("6"), List.of(
                new ScannedNoteBlock(FAR, 6, 9.0),
                new ScannedNoteBlock(NEAR, 6, 1.0)));

        assertEquals(Map.of(6, List.of(NEAR)), plan.blocksByNote());
    }

    @Test
    void recordsTuningClicksForBlocksOnTheWrongNote() {
        NoteBlockPlan plan = NoteBlockPlan.create(NotebotModule.parseSong("8"), List.of(
                new ScannedNoteBlock(NEAR, 6, 1.0)));

        assertEquals(Map.of(NEAR, 2), plan.tuneHits());
        assertEquals(Map.of(NEAR, 8), plan.targetNotes());
        assertEquals(Map.of(NEAR, 6), plan.observedNotes());
    }

    @Test
    void needsOneBlockPerNoteInAChord() {
        NoteBlockPlan plan = NoteBlockPlan.create(NotebotModule.parseSong("6+6"), List.of(
                new ScannedNoteBlock(NEAR, 6, 1.0)));

        assertFalse(plan.possible());
        assertEquals(2, plan.requiredBlocks());
    }
}
