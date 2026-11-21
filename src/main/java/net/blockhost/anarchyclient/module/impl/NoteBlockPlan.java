package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import net.minecraft.core.BlockPos;

/**
 * Which note block plays which note, and how many clicks each block needs to be tuned. Each note gets the
 * blocks that need the fewest clicks, nearest first.
 */
record NoteBlockPlan(
        boolean possible,
        int requiredBlocks,
        Map<Integer, List<BlockPos>> blocksByNote,
        Map<BlockPos, Integer> targetNotes,
        Map<BlockPos, Integer> observedNotes,
        SequencedMap<BlockPos, Integer> tuneHits) {

    /** A note block in reach, with the note it plays now. */
    record ScannedNoteBlock(BlockPos pos, int note, double distanceSq) {}

    static NoteBlockPlan create(final NotebotSong song, final List<ScannedNoteBlock> scannedBlocks) {
        int requiredBlocks = song.requiredBlocksByNote().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        if (scannedBlocks.size() < requiredBlocks) {
            return impossible(requiredBlocks);
        }
        Map<Integer, List<BlockPos>> blocksByNote = new HashMap<>();
        Map<BlockPos, Integer> targetNotes = new HashMap<>();
        Map<BlockPos, Integer> observedNotes = new HashMap<>();
        SequencedMap<BlockPos, Integer> tuneHits = new LinkedHashMap<>();
        ArrayDeque<ScannedNoteBlock> availableBlocks = new ArrayDeque<>(scannedBlocks);

        List<Integer> notes =
                song.requiredBlocksByNote().keySet().stream().sorted().toList();
        for (int note : notes) {
            int required = song.requiredBlocksByNote().get(note);
            for (int index = 0; index < required; index++) {
                ScannedNoteBlock block = removeBestBlock(availableBlocks, note);
                if (block == null) {
                    return impossible(requiredBlocks);
                }
                blocksByNote.computeIfAbsent(note, ignored -> new ArrayList<>()).add(block.pos());
                targetNotes.put(block.pos(), note);
                observedNotes.put(block.pos(), block.note());
                int clicks = NotebotSong.tuneClicks(block.note(), note);
                if (clicks > 0) {
                    tuneHits.put(block.pos(), clicks);
                }
            }
        }
        Map<Integer, List<BlockPos>> immutableBlocksByNote = new HashMap<>();
        blocksByNote.forEach((note, blocks) -> immutableBlocksByNote.put(note, List.copyOf(blocks)));
        return new NoteBlockPlan(
                true,
                requiredBlocks,
                Map.copyOf(immutableBlocksByNote),
                Map.copyOf(targetNotes),
                Map.copyOf(observedNotes),
                new LinkedHashMap<>(tuneHits));
    }

    private static ScannedNoteBlock removeBestBlock(final ArrayDeque<ScannedNoteBlock> blocks, final int targetNote) {
        ScannedNoteBlock best = null;
        for (ScannedNoteBlock block : blocks) {
            int blockClicks = NotebotSong.tuneClicks(block.note(), targetNote);
            int bestClicks = best == null ? Integer.MAX_VALUE : NotebotSong.tuneClicks(best.note(), targetNote);
            if (best == null
                    || blockClicks < bestClicks
                    || (blockClicks == bestClicks && block.distanceSq() < best.distanceSq())) {
                best = block;
            }
        }
        if (best != null) {
            blocks.remove(best);
        }
        return best;
    }

    private static NoteBlockPlan impossible(final int requiredBlocks) {
        return new NoteBlockPlan(false, requiredBlocks, Map.of(), Map.of(), Map.of(), new LinkedHashMap<>());
    }
}
