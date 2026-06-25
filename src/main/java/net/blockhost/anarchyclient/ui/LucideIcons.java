package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

final class LucideIcons {

    private static final String RESOURCE_PATH = "/assets/anarchyclient/font/lucide-icons.properties";
    private static final Set<String> MISSING_ICONS = ConcurrentHashMap.newKeySet();

    private static volatile Map<String, String> glyphs;

    private LucideIcons() {
    }

    static String glyph(final String name) {
        String glyph = glyphs().get(name);
        if (glyph == null) {
            if (MISSING_ICONS.add(name)) {
                AnarchyClient.LOGGER.warn("Unknown Lucide icon '{}'", name);
            }
            return "";
        }
        return glyph;
    }

    static Map<String, String> parse(final InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));

        Map<String, String> parsed = new TreeMap<>();
        for (String name : properties.stringPropertyNames()) {
            parsed.put(name, glyphFromHex(properties.getProperty(name)));
        }
        return Map.copyOf(parsed);
    }

    private static Map<String, String> glyphs() {
        Map<String, String> local = glyphs;
        if (local != null) {
            return local;
        }
        synchronized (LucideIcons.class) {
            local = glyphs;
            if (local == null) {
                local = load();
                glyphs = local;
            }
            return local;
        }
    }

    private static Map<String, String> load() {
        try (InputStream input = LucideIcons.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                AnarchyClient.LOGGER.warn("Lucide icon map resource is missing: {}", RESOURCE_PATH);
                return Map.of();
            }
            return parse(input);
        } catch (IOException | IllegalArgumentException exception) {
            AnarchyClient.LOGGER.warn("Failed to load Lucide icon map from {}", RESOURCE_PATH, exception);
            return Map.of();
        }
    }

    private static String glyphFromHex(final String hex) {
        int codePoint = Integer.parseInt(hex.trim(), 16);
        return new String(Character.toChars(codePoint));
    }
}
