package net.blockhost.anarchyclient.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.AnarchyClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central position store for HUD elements. Modules ask {@link #origin} for their top-left corner each
 * frame (defaulting to a screen corner until the user drags them) and the {@link HudEditorScreen}
 * reads back the recorded {@link Element} bounds to draw draggable handles.
 *
 * <p>Positions are absolute virtual pixels clamped to the screen on render, so they survive a
 * resolution change without a fractional-anchor scheme.
 */
public final class HudLayout {

    private static final int MARGIN = 6;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-hud.json");

    /** User-dragged top-left positions, keyed by module id. Absent = use the default corner. */
    private static final Map<String, int[]> positions = new LinkedHashMap<>();
    /** Bounds recorded during the most recent render pass, for the editor. */
    private static final Map<String, Element> bounds = new LinkedHashMap<>();

    private HudLayout() {
    }

    public record Element(String id, String name, int x, int y, int width, int height) {

        public boolean contains(final double px, final double py) {
            return px >= this.x && px <= this.x + this.width && py >= this.y && py <= this.y + this.height;
        }
    }

    /**
     * Resolve the top-left corner for a HUD element and record its bounds for the editor.
     */
    public static int[] origin(final String id, final String name, final int width, final int height,
                               final String defaultCorner, final GuiGraphicsExtractor graphics) {
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        int[] pos = positions.get(id);
        int x;
        int y;
        if (pos != null) {
            x = pos[0];
            y = pos[1];
        } else {
            x = defaultCorner.endsWith("Right") ? guiWidth - width - MARGIN : MARGIN;
            y = defaultCorner.startsWith("Bottom") ? guiHeight - height - MARGIN : MARGIN;
        }
        // Clamp both paths: a wide element (long text, small resolution) pushes the default corner
        // negative just as easily as a stale stored position does.
        x = clamp(x, 0, Math.max(0, guiWidth - width));
        y = clamp(y, 0, Math.max(0, guiHeight - height));
        bounds.put(id, new Element(id, name, x, y, width, height));
        return new int[]{x, y};
    }

    /** Editor: clear the recorded bounds before a fresh preview render pass. */
    static void clearBounds() {
        bounds.clear();
    }

    /** Editor: snapshot of the elements rendered in the last pass. */
    static List<Element> elements() {
        return List.copyOf(bounds.values());
    }

    /** Editor: move an element's top-left to an absolute position. */
    static void move(final String id, final int x, final int y) {
        positions.put(id, new int[]{x, y});
    }

    /** Editor: return an element to its default corner. */
    static void reset(final String id) {
        positions.remove(id);
    }

    public static void load() {
        positions.clear();
        if (!Files.exists(PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }
            for (String id : root.keySet()) {
                JsonObject entry = root.getAsJsonObject(id);
                if (entry != null && entry.has("x") && entry.has("y")) {
                    positions.put(id, new int[]{entry.get("x").getAsInt(), entry.get("y").getAsInt()});
                }
            }
        } catch (RuntimeException | IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to load HUD layout from {}", PATH, exception);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, int[]> entry : positions.entrySet()) {
            JsonObject value = new JsonObject();
            value.addProperty("x", entry.getValue()[0]);
            value.addProperty("y", entry.getValue()[1]);
            root.add(entry.getKey(), value);
        }
        try {
            Path parent = PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save HUD layout to {}", PATH, exception);
        }
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }
}
