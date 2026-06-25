package net.blockhost.anarchyclient.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blockhost.anarchyclient.AnarchyClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class WaypointStore {

    public static final int DEFAULT_COLOR = 0x78E6FFD2;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();

    public WaypointStore(final Path path) {
        this.path = path;
    }

    public static Path defaultPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("anarchyclient")
                .resolve("waypoints.json");
    }

    public synchronized void load() {
        this.waypoints.clear();
        if (!Files.exists(this.path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(this.path)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (element == null || !element.isJsonObject()) {
                return;
            }
            JsonArray entries = element.getAsJsonObject().getAsJsonArray("waypoints");
            if (entries == null) {
                return;
            }
            for (JsonElement entry : entries) {
                readWaypoint(entry).ifPresent(waypoint -> this.waypoints.put(key(waypoint.world(), waypoint.name()), waypoint));
            }
        } catch (IOException | RuntimeException exception) {
            AnarchyClient.LOGGER.warn("Failed to load AnarchyClient waypoints from {}", this.path, exception);
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject();
        JsonArray entries = new JsonArray();
        for (Waypoint waypoint : this.sorted()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("world", waypoint.world());
            entry.addProperty("name", waypoint.name());
            entry.addProperty("x", waypoint.pos().getX());
            entry.addProperty("y", waypoint.pos().getY());
            entry.addProperty("z", waypoint.pos().getZ());
            entry.addProperty("color", waypoint.color());
            entries.add(entry);
        }
        root.add("waypoints", entries);
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(this.path)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save AnarchyClient waypoints to {}", this.path, exception);
        }
    }

    public synchronized Waypoint add(final Waypoint waypoint) {
        this.waypoints.put(key(waypoint.world(), waypoint.name()), waypoint);
        this.save();
        return waypoint;
    }

    public synchronized boolean remove(final String world, final String name) {
        boolean removed = this.waypoints.remove(key(world, name)) != null;
        if (removed) {
            this.save();
        }
        return removed;
    }

    public synchronized Optional<Waypoint> find(final String world, final String name) {
        return Optional.ofNullable(this.waypoints.get(key(world, name)));
    }

    public synchronized List<Waypoint> byWorld(final String world) {
        return this.sorted().stream()
                .filter(waypoint -> waypoint.world().equals(world))
                .toList();
    }

    public synchronized List<Waypoint> all() {
        return this.sorted();
    }

    public static String currentWorld(final Minecraft client) {
        if (client == null) {
            return "unknown";
        }
        String dimension = client.level == null ? "unknown" : client.level.dimension().identifier().toString();
        ServerData server = client.getCurrentServer();
        if (server != null) {
            return "server:" + safe(server.ip) + "|" + dimension;
        }
        return "singleplayer:" + dimension;
    }

    private List<Waypoint> sorted() {
        return this.waypoints.values().stream()
                .sorted(Comparator.comparing(Waypoint::world).thenComparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Optional<Waypoint> readWaypoint(final JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject entry = element.getAsJsonObject();
        try {
            String world = string(entry, "world", "unknown");
            String name = string(entry, "name", "waypoint");
            int x = entry.get("x").getAsInt();
            int y = entry.get("y").getAsInt();
            int z = entry.get("z").getAsInt();
            int color = entry.has("color") ? entry.get("color").getAsInt() : DEFAULT_COLOR;
            return Optional.of(new Waypoint(world, name, new BlockPos(x, y, z), color));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public static List<Waypoint> parseLegacy(final String world, final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Waypoint> result = new ArrayList<>();
        for (String token : value.split(";")) {
            String[] parts = token.trim().split(":");
            if (parts.length != 4) {
                continue;
            }
            try {
                result.add(new Waypoint(world, parts[0], new BlockPos(
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())
                ), DEFAULT_COLOR));
            } catch (NumberFormatException ignored) {
            }
        }
        return List.copyOf(result);
    }

    private static String string(final JsonObject object, final String name, final String fallback) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        String value = element.getAsString();
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String key(final String world, final String name) {
        return safe(world).toLowerCase(Locale.ROOT) + "\n" + safe(name).toLowerCase(Locale.ROOT);
    }

    private static String safe(final String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
