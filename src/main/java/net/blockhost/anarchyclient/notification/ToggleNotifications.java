package net.blockhost.anarchyclient.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/**
 * BleachHack-style module toggle popups: a small stack of "Module enabled/disabled" toasts in a
 * screen corner that slide in and fade out. State is global and persisted through {@code ClientConfig}.
 */
public final class ToggleNotifications {

    private static final int ROW_HEIGHT = 14;
    private static final int ROW_GAP = 2;
    private static final int MARGIN = 4;
    private static final int MAX_TOASTS = 8;
    private static final int FADE_IN_MS = 150;
    private static final int FADE_OUT_MS = 250;
    private static final List<Integer> DURATIONS = List.of(1000, 2000, 3000, 5000);

    private static final List<Toast> TOASTS = new ArrayList<>();

    private static boolean enabled = true;
    private static Corner corner = Corner.TOP_RIGHT;
    private static int durationMs = 2000;

    private ToggleNotifications() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enabled(final boolean value) {
        enabled = value;
    }

    public static Corner corner() {
        return corner;
    }

    public static void corner(final Corner value) {
        corner = value == null ? Corner.TOP_RIGHT : value;
    }

    public static int durationMs() {
        return durationMs;
    }

    public static void durationMs(final int value) {
        durationMs = Math.max(250, value);
    }

    /** Advances the configured duration to the next preset (1s, 2s, 3s, 5s). */
    public static void cycleDuration() {
        int index = DURATIONS.indexOf(durationMs);
        durationMs = DURATIONS.get((index + 1) % DURATIONS.size());
    }

    /** Queues a toast for a module toggle. No-op while notifications are off. */
    public static void push(final String moduleName, final boolean on) {
        if (!enabled || moduleName == null) {
            return;
        }
        // Re-toggling a module refreshes its existing toast instead of stacking a duplicate.
        TOASTS.removeIf(toast -> toast.name.equals(moduleName));
        TOASTS.add(new Toast(moduleName, on, System.currentTimeMillis()));
        while (TOASTS.size() > MAX_TOASTS) {
            TOASTS.removeFirst();
        }
    }

    public static void render(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (!enabled || client == null || client.player == null || TOASTS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        TOASTS.removeIf(toast -> now - toast.createdAt >= durationMs);
        Font font = client.font;
        boolean right = corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
        boolean bottom = corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;
        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();
        List<Toast> snapshot = new ArrayList<>(TOASTS);
        for (int index = 0; index < snapshot.size(); index++) {
            Toast toast = snapshot.get(index);
            long elapsed = now - toast.createdAt;
            float alpha = alpha(elapsed);
            String text = toast.name + (toast.on ? " enabled" : " disabled");
            int width = font.width(text) + 10;
            int slide = (int) (slide(elapsed) * width);
            int y = bottom
                    ? guiHeight - MARGIN - ROW_HEIGHT - index * (ROW_HEIGHT + ROW_GAP)
                    : MARGIN + index * (ROW_HEIGHT + ROW_GAP);
            int x = right ? guiWidth - width - MARGIN + slide : MARGIN - slide;
            int alphaBits = alphaBits(alpha);
            graphics.fill(x, y, x + width, y + ROW_HEIGHT, argb(alpha, 0.78F) << 24 | 0x101418);
            int barWidth = 2;
            int barX = right ? x + width - barWidth : x;
            int bar = toast.on ? 0x55E08A : 0xE05555;
            graphics.fill(barX, y, barX + barWidth, y + ROW_HEIGHT, alphaBits | bar);
            graphics.text(font, text, x + (right ? 4 : 6), y + (ROW_HEIGHT - 8) / 2, alphaBits | 0xFFFFFF, true);
        }
    }

    private static float slide(final long elapsed) {
        if (elapsed < FADE_IN_MS) {
            return 1F - elapsed / (float) FADE_IN_MS;
        }
        if (elapsed > durationMs - FADE_OUT_MS) {
            return clamp01((elapsed - (durationMs - FADE_OUT_MS)) / (float) FADE_OUT_MS);
        }
        return 0F;
    }

    private static float alpha(final long elapsed) {
        if (elapsed < FADE_IN_MS) {
            return clamp01(elapsed / (float) FADE_IN_MS);
        }
        if (elapsed > durationMs - FADE_OUT_MS) {
            return clamp01((durationMs - elapsed) / (float) FADE_OUT_MS);
        }
        return 1F;
    }

    private static int alphaBits(final float alpha) {
        return argb(alpha, 1F) << 24;
    }

    private static int argb(final float alpha, final float scale) {
        return Math.round(clamp01(alpha) * scale * 255F) & 0xFF;
    }

    private static float clamp01(final float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    public enum Corner {
        TOP_RIGHT("Top Right"),
        TOP_LEFT("Top Left"),
        BOTTOM_RIGHT("Bottom Right"),
        BOTTOM_LEFT("Bottom Left");

        private final String displayName;

        Corner(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return this.displayName;
        }

        public Corner next() {
            Corner[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Corner fromName(final String name) {
            if (name != null) {
                for (Corner value : values()) {
                    if (value.name().equalsIgnoreCase(name) || value.displayName.equalsIgnoreCase(name)) {
                        return value;
                    }
                }
            }
            return TOP_RIGHT;
        }
    }

    private record Toast(String name, boolean on, long createdAt) {
    }
}
