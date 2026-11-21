package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Notebot song: the notes to play on each tick, and how many note blocks each note needs. Notes run from
 * 0 to 24, the 25 pitches of a note block.
 */
record NotebotSong(Map<Integer, List<Integer>> notesByTick, int lastTick, Map<Integer, Integer> requiredBlocksByNote) {

    static final NotebotSong EMPTY = new NotebotSong(Map.of(), 0, Map.of());

    boolean empty() {
        return this.notesByTick.isEmpty();
    }

    /**
     * Parses notes separated by spaces, commas or semicolons. {@code +} joins notes into a chord, and
     * {@code tick:note} places a note on an explicit tick.
     */
    static NotebotSong parse(final String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        Map<Integer, List<Integer>> notes = new LinkedHashMap<>();
        int nextTick = 0;
        for (String token : text.split("[,;\\s]+", -1)) {
            if (token.isBlank()) {
                continue;
            }
            String noteToken = token.trim();
            int tick = nextTick;
            int colon = noteToken.indexOf(':');
            if (colon >= 0) {
                try {
                    tick = Math.max(
                            0, Integer.parseInt(noteToken.substring(0, colon).trim()));
                    noteToken = noteToken.substring(colon + 1);
                } catch (RuntimeException exception) {
                    continue;
                }
            }
            List<Integer> tickNotes = notes.computeIfAbsent(tick, ignored -> new ArrayList<>());
            for (String part : noteToken.split("\\+", -1)) {
                if (!part.isBlank()) {
                    tickNotes.add(parseNote(part));
                }
            }
            nextTick = tick + 1;
        }
        notes.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (notes.isEmpty()) {
            return EMPTY;
        }
        int lastTick = notes.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        Map<Integer, List<Integer>> immutableNotes = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : notes.entrySet()) {
            immutableNotes.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new NotebotSong(Map.copyOf(immutableNotes), lastTick, requirements(immutableNotes));
    }

    static int parseNote(final String value) {
        try {
            return parseNoteToken(value);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    static float pitch(final int note) {
        return (float) Math.pow(2.0, (Math.max(0, Math.min(24, note)) - 12) / 12.0);
    }

    static int pitchToNote(final float pitch) {
        if (pitch <= 0.0F || !Float.isFinite(pitch)) {
            return 0;
        }
        return Math.max(0, Math.min(24, Math.round(12.0F + 12.0F * (float) (Math.log(pitch) / Math.log(2.0)))));
    }

    static int tuneClicks(final int from, final int to) {
        int safeFrom = Math.max(0, Math.min(24, from));
        int safeTo = Math.max(0, Math.min(24, to));
        return safeFrom <= safeTo ? safeTo - safeFrom : 25 - safeFrom + safeTo;
    }

    private static Map<Integer, Integer> requirements(final Map<Integer, List<Integer>> notesByTick) {
        Map<Integer, Integer> requirements = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        for (List<Integer> notes : notesByTick.values()) {
            counts.clear();
            for (int note : notes) {
                counts.merge(note, 1, Integer::sum);
            }
            counts.forEach((note, count) -> requirements.merge(note, count, Math::max));
        }
        return Map.copyOf(requirements);
    }

    private static int parseNoteToken(final String value) {
        String token = value.trim().toLowerCase(Locale.ROOT);
        if (token.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(24, Integer.parseInt(token)));
        } catch (NumberFormatException ignored) {
            return switch (token) {
                case "c", "c4" -> 6;
                case "c#", "db", "c#4", "db4" -> 7;
                case "d", "d4" -> 8;
                case "d#", "eb", "d#4", "eb4" -> 9;
                case "e", "e4" -> 10;
                case "f", "f4" -> 11;
                case "f#", "gb", "f#4", "gb4" -> 12;
                case "g", "g4" -> 13;
                case "g#", "ab", "g#4", "ab4" -> 14;
                case "a", "a4" -> 15;
                case "a#", "bb", "a#4", "bb4" -> 16;
                case "b", "b4" -> 17;
                default -> 0;
            };
        }
    }
}
