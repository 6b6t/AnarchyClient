package net.blockhost.anarchyclient.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ServerListTools {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ServerListTools() {
    }

    static ServerList load(final Minecraft client) {
        ServerList list = new ServerList(client);
        list.load();
        return list;
    }

    static List<ServerEntry> entries(final ServerList list) {
        List<ServerEntry> entries = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            ServerData server = list.get(index);
            entries.add(new ServerEntry(server.name, server.ip));
        }
        return List.copyOf(entries);
    }

    static int addOrUpdate(final ServerList list, final String address, final String name) {
        ServerEntry entry = new ServerEntry(normalizeName(name, address), normalizeAddress(address));
        for (int index = 0; index < list.size(); index++) {
            ServerData existing = list.get(index);
            if (normalizeAddress(existing.ip).equals(entry.address())) {
                existing.name = entry.name();
                existing.ip = entry.address();
                return 0;
            }
        }
        list.add(new ServerData(entry.name(), entry.address(), ServerData.Type.OTHER), false);
        return 1;
    }

    static int cleanup(final ServerList list) {
        Map<String, ServerData> firstByAddress = new LinkedHashMap<>();
        List<ServerData> duplicates = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            ServerData server = list.get(index);
            String normalized = normalizeAddress(server.ip);
            if (firstByAddress.putIfAbsent(normalized, server) != null) {
                duplicates.add(server);
            }
        }
        duplicates.forEach(list::remove);
        return duplicates.size();
    }

    static int importJson(final ServerList list, final Path path) throws IOException {
        int added = 0;
        for (ServerEntry entry : readJson(path)) {
            added += addOrUpdate(list, entry.address(), entry.name());
        }
        return added;
    }

    static Path exportJson(final ServerList list, final Path path) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            JsonArray array = new JsonArray();
            for (ServerEntry entry : entries(list)) {
                JsonObject object = new JsonObject();
                object.addProperty("name", entry.name());
                object.addProperty("address", entry.address());
                array.add(object);
            }
            GSON.toJson(array, writer);
        }
        return path;
    }

    static List<ServerEntry> readJson(final Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root == null || !root.isJsonArray()) {
                throw new IOException("expected a JSON array");
            }
            List<ServerEntry> entries = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String address = object.has("address") ? object.get("address").getAsString() : "";
                if (address.isBlank()) {
                    continue;
                }
                String name = object.has("name") ? object.get("name").getAsString() : address;
                entries.add(new ServerEntry(normalizeName(name, address), normalizeAddress(address)));
            }
            return List.copyOf(entries);
        } catch (RuntimeException exception) {
            throw new IOException("invalid server-list export", exception);
        }
    }

    static String normalizeAddress(final String address) {
        if (address == null) {
            return "";
        }
        return address.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(final String name, final String address) {
        if (name == null || name.isBlank()) {
            return address == null || address.isBlank() ? "Minecraft Server" : address.trim();
        }
        String trimmed = name.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    record ServerEntry(String name, String address) {
    }
}
