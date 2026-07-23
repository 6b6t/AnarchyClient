package net.blockhost.anarchyclient.notification;

import net.blockhost.anarchyclient.rivet.Blaze3DRenderer;
import net.blockhost.anarchyclient.rivet.SoftShadowCommand;
import net.lenni0451.commons.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * BleachHack-style module toggle feedback. Either a stack of glass toasts in a screen corner (POPUP)
 * or a styled chat line (CHAT). Colors follow the client's theme accent so it matches the menu.
 * State is global and persisted through {@code ClientConfig}.
 */
public final class ToggleNotifications {

    private static final int ROW_HEIGHT = 17;
    private static final int ROW_GAP = 5;
    private static final int MARGIN = 6;
    private static final int MAX_TOASTS = 8;
    private static final int FADE_IN_MS = 150;
    private static final int FADE_OUT_MS = 250;
    private static final List<Integer> DURATIONS = List.of(1000, 2000, 3000, 5000);

    // On-brand tokens mirrored from the menu's GlassTheme so the toast reads as the same client.
    private static final int TEXT_RGB = 0xFFFFFF;
    private static final int DISABLED_RGB = 0xE05563;
    private static final int DEFAULT_ACCENT = 0x00BA94;
    private static final int DEFAULT_GLASS = 0xB00D111A;

    private static final List<Toast> TOASTS = new ArrayList<>();

    private static boolean enabled = true;
    private static Mode mode = Mode.POPUP;
    private static Corner corner = Corner.TOP_RIGHT;
    private static int durationMs = 2000;
    private static int accent = DEFAULT_ACCENT;
    // Corner radius and glass tint mirror the Theme tab globals, pushed from GlassTheme.
    private static float cornerRadius = 14F;
    private static int glass = DEFAULT_GLASS;

    private ToggleNotifications() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void enabled(final boolean value) {
        enabled = value;
    }

    public static Mode mode() {
        return mode;
    }

    public static void mode(final Mode value) {
        mode = value == null ? Mode.POPUP : value;
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

    /**
     * Mirror the menu's live design tokens (accent, corner radius, glass tint) so popups match the
     * Theme tab. Pushed from GlassTheme whenever any of them change.
     */
    public static void theme(final int accentArgb, final float radius, final int glassArgb) {
        accent = accentArgb;
        cornerRadius = radius;
        glass = glassArgb;
    }

    /** Emits feedback for a module toggle. No-op while notifications are off. */
    public static void push(final String moduleName, final boolean on) {
        if (!enabled || moduleName == null) {
            return;
        }
        if (mode == Mode.CHAT) {
            sendChat(moduleName, on);
            return;
        }
        // Re-toggling a module refreshes its existing toast instead of stacking a duplicate.
        TOASTS.removeIf(toast -> toast.name.equals(moduleName));
        TOASTS.add(new Toast(moduleName, on, System.currentTimeMillis()));
        while (TOASTS.size() > MAX_TOASTS) {
            TOASTS.removeFirst();
        }
    }

    private static void sendChat(final String moduleName, final boolean on) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        int accentRgb = accent & 0xFFFFFF;
        Component message = Component.literal("")
                .append(Component.literal("[AnarchyClient] ").withStyle(style -> style.withColor(accentRgb).withBold(true)))
                .append(Component.literal(moduleName + " "))
                .append(Component.literal(on ? "enabled" : "disabled")
                        .withStyle(style -> style.withColor(on ? accentRgb : DISABLED_RGB)));
        client.player.sendSystemMessage(message);
    }

    public static void render(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (!enabled || mode != Mode.POPUP || client == null || client.player == null || TOASTS.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        TOASTS.removeIf(toast -> now - toast.createdAt >= durationMs);
        Font font = client.font;
        boolean right = corner == Corner.TOP_RIGHT || corner == Corner.BOTTOM_RIGHT;
        boolean bottom = corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT;
        float guiWidth = graphics.guiWidth();
        float guiHeight = graphics.guiHeight();
        Blaze3DRenderer renderer = new Blaze3DRenderer(client, graphics);
        List<Toast> snapshot = new ArrayList<>(TOASTS);
        try {
            for (int index = 0; index < snapshot.size(); index++) {
                Toast toast = snapshot.get(index);
                long elapsed = now - toast.createdAt;
                float alpha = alpha(elapsed);
                String text = toast.name + (toast.on ? " enabled" : " disabled");
                float width = font.width(text) + 20F;
                float offset = slide(elapsed) * (width + MARGIN);
                float y = bottom
                        ? guiHeight - MARGIN - ROW_HEIGHT - index * (ROW_HEIGHT + ROW_GAP)
                        : MARGIN + index * (ROW_HEIGHT + ROW_GAP);
                float x = right ? guiWidth - width - MARGIN + offset : MARGIN - offset;
                // Follow the Theme tab's corner radius, capped so a small toast never becomes a pill.
                float radius = Math.min(cornerRadius, Math.min(width, ROW_HEIGHT) / 2F);

                // Same soft shadow + anti-aliased glass panel the menu draws for its cards.
                renderer.custom(new SoftShadowCommand(x, y, width, ROW_HEIGHT, radius, 8F, 2F,
                        Color.fromRGBA(0, 0, 0, scaleAlpha(alpha, 0.43F))));
                renderer.optimizedFillRoundedRect(x, y, width, ROW_HEIGHT, radius, glassColor(alpha));
                renderer.outlineRoundedRect(x, y, width, ROW_HEIGHT, radius, 1F,
                        Color.fromRGBA(255, 255, 255, scaleAlpha(alpha, 0.11F)));
                // Accent stripe on the left, matching the menu's active-row highlight.
                Color stripe = toast.on ? withAlpha(accent, alpha) : Color.fromRGBA(224, 85, 99, scaleAlpha(alpha, 1F));
                renderer.fillRoundedRect(x + 3F, y + 4F, 2F, ROW_HEIGHT - 8F, 1F, stripe);

                int textArgb = alphaBits(alpha) | TEXT_RGB;
                graphics.text(font, text, Math.round(x + 9F), Math.round(y + (ROW_HEIGHT - 8F) / 2F), textArgb, false);
            }
        } catch (RuntimeException ignored) {
            // A render failure must never take down the HUD; the toast simply won't show this frame.
        }
    }

    private static Color withAlpha(final int rgb, final float alpha) {
        return Color.fromRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, scaleAlpha(alpha, 1F));
    }

    /** Glass tint from the Theme tab (its opacity is the base alpha), further scaled by the fade. */
    private static Color glassColor(final float fade) {
        int baseAlpha = (glass >> 24) & 0xFF;
        return Color.fromRGBA((glass >> 16) & 0xFF, (glass >> 8) & 0xFF, glass & 0xFF,
                Math.round(baseAlpha * clamp01(fade)));
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
        return scaleAlpha(alpha, 1F) << 24;
    }

    private static int scaleAlpha(final float alpha, final float scale) {
        return Math.round(clamp01(alpha) * scale * 255F) & 0xFF;
    }

    private static float clamp01(final float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    public enum Mode {
        POPUP("Popup"),
        CHAT("Chat");

        private final String displayName;

        Mode(final String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return this.displayName;
        }

        public Mode next() {
            Mode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }

        public static Mode fromName(final String name) {
            if (name != null) {
                for (Mode value : values()) {
                    if (value.name().equalsIgnoreCase(name) || value.displayName.equalsIgnoreCase(name)) {
                        return value;
                    }
                }
            }
            return POPUP;
        }
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
