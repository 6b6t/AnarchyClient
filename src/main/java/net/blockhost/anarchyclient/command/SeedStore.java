package net.blockhost.anarchyclient.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import net.blockhost.anarchyclient.config.ConfigFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

final class SeedStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SeedStore() {}

    static List<SeedRecord> list(final Path path) throws IOException {
        return read(path).entrySet().stream()
                .filter(entry -> entry.getValue().isJsonObject())
                .map(entry -> record(entry.getKey(), entry.getValue().getAsJsonObject()))
                .filter(record -> !record.seed().isBlank())
                .sorted(Comparator.comparing(SeedRecord::world))
                .toList();
    }

    static @Nullable SeedRecord get(final Path path, final String world) throws IOException {
        JsonElement entry = read(path).get(world);
        if (entry == null || !entry.isJsonObject()) {
            return null;
        }
        SeedRecord record = record(world, entry.getAsJsonObject());
        return record.seed().isBlank() ? null : record;
    }

    static SeedRecord put(final Path path, final String world, final String seed, final Instant savedAt)
            throws IOException {
        return put(path, world, seed, savedAt, "", "", "manual");
    }

    static SeedRecord put(
            final Path path,
            final String world,
            final String seed,
            final Instant savedAt,
            final String version,
            final String dimension,
            final String source)
            throws IOException {
        String normalized = normalizeSeed(seed);
        JsonObject root = read(path);
        JsonObject entry = new JsonObject();
        entry.addProperty("seed", normalized);
        entry.addProperty("saved_at", savedAt.toString());
        entry.addProperty("version", normalizeMetadata(version));
        entry.addProperty("dimension", normalizeMetadata(dimension));
        entry.addProperty("source", normalizeMetadata(source).isBlank() ? "manual" : normalizeMetadata(source));
        root.add(world, entry);
        write(path, root);
        return new SeedRecord(
                world,
                normalized,
                savedAt.toString(),
                normalizeMetadata(version),
                normalizeMetadata(dimension),
                normalizeMetadata(source).isBlank() ? "manual" : normalizeMetadata(source));
    }

    static boolean delete(final Path path, final String world) throws IOException {
        JsonObject root = read(path);
        boolean removed = root.remove(world) != null;
        if (removed) {
            write(path, root);
        }
        return removed;
    }

    static String normalizeSeed(final String seed) {
        if (seed == null || seed.isBlank()) {
            throw new IllegalArgumentException("Seed cannot be blank.");
        }
        return seed.trim();
    }

    static String normalizeMetadata(final String value) {
        return value == null ? "" : value.trim();
    }

    static String worldKey(final Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null) {
            return worldKey(server.ip);
        }
        IntegratedServer singleplayer = client.getSingleplayerServer();
        if (singleplayer != null) {
            // One key per save folder: a world has one seed in every dimension, and different worlds must not
            // share a saved seed.
            Path folder = singleplayer
                    .getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize()
                    .getFileName();
            return "singleplayer:" + (folder == null ? "world" : folder);
        }
        return "unknown";
    }

    static String worldKey(final String serverAddress) {
        return "server:" + (serverAddress == null || serverAddress.isBlank() ? "unknown" : serverAddress.trim());
    }

    private static JsonObject read(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return new JsonObject();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("invalid seed file", exception);
        }
    }

    private static void write(final Path path, final JsonObject root) throws IOException {
        ConfigFiles.writeAtomically(path, GSON.toJson(root));
    }

    private static SeedRecord record(final String world, final JsonObject entry) {
        return new SeedRecord(
                world,
                string(entry, "seed"),
                string(entry, "saved_at"),
                string(entry, "version"),
                string(entry, "dimension"),
                string(entry, "source"));
    }

    /** Reads a string field, treating a missing or non-text value (from a hand-edited file) as empty. */
    private static String string(final JsonObject entry, final String key) {
        JsonElement value = entry.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString().trim() : "";
    }

    record SeedRecord(String world, String seed, String savedAt, String version, String dimension, String source) {}
}
