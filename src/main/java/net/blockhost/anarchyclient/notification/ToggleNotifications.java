package net.blockhost.anarchyclient.notification;

import net.blockhost.anarchyclient.rivet.Blaze3DRenderer;
import net.blockhost.anarchyclient.rivet.GlassPanelCommand;
import net.blockhost.anarchyclient.ui.GlassTheme;
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

    private static final int ROW_HEIGHT = 20;
    /** Text inset, and therefore the chip's horizontal padding. */
    private static final float TEXT_INSET = 12F;
    private static final Color OUTLINE = Color.fromRGBA(255, 255, 255, 70);
    private static final int ROW_GAP = 5;
    private static final int MARGIN = 6;
    private static final int MAX_TOASTS = 8;
    private static final int FADE_IN_MS = 150;
    private static final int FADE_OUT_MS = 250;
    private static final List<Integer> DURATIONS = List.of(1000, 2000, 3000, 5000);

    private static final List<Toast> TOASTS = new ArrayList<>();

    private static boolean enabled = true;
    private static Mode mode = Mode.POPUP;
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
        int accentRgb = GlassTheme.accent().toARGB() & 0xFFFFFF;
        int offRgb = GlassTheme.OFF.toARGB() & 0xFFFFFF;
        Component message = Component.literal("")
                .append(Component.literal("[AnarchyClient] ").withStyle(style -> style.withColor(accentRgb).withBold(true)))
                .append(Component.literal(moduleName + " "))
                .append(Component.literal(on ? "enabled" : "disabled")
                        .withStyle(style -> style.withColor(on ? accentRgb : offRgb)));
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
        Blaze3DRenderer renderer = new Blaze3DRenderer(client, graphics);
        float guiWidth = graphics.guiWidth();
        float guiHeight = graphics.guiHeight();
        List<Toast> snapshot = new ArrayList<>(TOASTS);
        try {
            for (int index = 0; index < snapshot.size(); index++) {
                Toast toast = snapshot.get(index);
                long elapsed = now - toast.createdAt;
                float alpha = alpha(elapsed);
                String text = toast.name + (toast.on ? " enabled" : " disabled");
                float width = font.width(text) + TEXT_INSET * 2F;
                float offset = slide(elapsed) * (width + MARGIN);
                float y = bottom
                        ? guiHeight - MARGIN - ROW_HEIGHT - index * (ROW_HEIGHT + ROW_GAP)
                        : MARGIN + index * (ROW_HEIGHT + ROW_GAP);
                float x = right ? guiWidth - width - MARGIN + offset : MARGIN - offset;
                float radius = Math.min(GlassTheme.cornerRadius(), ROW_HEIGHT / 2F);

                // The HUD editor's hint chip, minus its drop shadow: in-game the panel has no blurred
                // scene to sample and stays translucent, so a shadow underneath shows straight through
                // it — brightest at the rim, darkest at the center. The rim carries the edge instead.
                renderer.custom(new GlassPanelCommand(x, y, width, ROW_HEIGHT, radius,
                        fade(GlassTheme.glass(), alpha)));
                // One 1px ring on the panel's own bounds, at the width sdf_outline renders, so it frames
                // the fill exactly instead of stacking a second ring inside it.
                renderer.outlineRoundedRect(x, y, width, ROW_HEIGHT, radius, 1F, fade(OUTLINE, alpha));
                graphics.text(font, text, Math.round(x + TEXT_INSET), Math.round(y + (ROW_HEIGHT - 8F) / 2F),
                        fade(GlassTheme.TEXT, alpha).toARGB(), false);
            }
        } catch (RuntimeException ignored) {
            // A render failure must never take down the HUD; the toast simply won't show this frame.
        }
    }

    private static Color fade(final Color color, final float alpha) {
        return color.multiplyAlpha(clamp01(alpha));
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
