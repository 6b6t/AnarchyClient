package net.blockhost.anarchyclient.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

final class SeedStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SeedStore() {
    }

    static List<SeedRecord> list(final Path path) throws IOException {
        return read(path).entrySet().stream()
                .filter(entry -> entry.getValue().isJsonObject())
                .map(entry -> record(entry.getKey(), entry.getValue().getAsJsonObject()))
                .sorted(Comparator.comparing(SeedRecord::world))
                .toList();
    }

    static SeedRecord get(final Path path, final String world) throws IOException {
        JsonObject entry = read(path).getAsJsonObject(world);
        return entry == null ? null : record(world, entry);
    }

    static SeedRecord put(final Path path, final String world, final String seed, final Instant savedAt) throws IOException {
        String normalized = normalizeSeed(seed);
        JsonObject root = read(path);
        JsonObject entry = new JsonObject();
        entry.addProperty("seed", normalized);
        entry.addProperty("saved_at", savedAt.toString());
        root.add(world, entry);
        write(path, root);
        return new SeedRecord(world, normalized, savedAt.toString());
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

    static String worldKey(final Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null) {
            return worldKey(server.ip);
        }
        if (client.level != null) {
            return "singleplayer:" + client.level.dimension().identifier();
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
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static SeedRecord record(final String world, final JsonObject entry) {
        String seed = entry.has("seed") ? entry.get("seed").getAsString() : "";
        String savedAt = entry.has("saved_at") ? entry.get("saved_at").getAsString() : "";
        return new SeedRecord(world, seed, savedAt);
    }

    record SeedRecord(String world, String seed, String savedAt) {
    }
}
