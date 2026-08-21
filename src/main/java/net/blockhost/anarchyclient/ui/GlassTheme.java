package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.lenni0451.commons.color.Color;
import net.minecraft.client.Minecraft;

/**
 * Global design tokens for the client. Every component reads these live — the menu through Rivet, the
 * toast popups and HUD text colors directly — so edits made in the Theme tab restyle the whole client
 * immediately. The editable values are persisted through {@link ClientConfig.UiPreferences}.
 */
public final class GlassTheme {

    // Fixed tokens.
    public static final Color TEXT = Color.fromRGBA(255, 255, 255, 245);
    public static final Color MUTED = Color.fromRGBA(255, 255, 255, 180);
    public static final Color FAINT = Color.fromRGBA(255, 255, 255, 120);
    static final Color DIVIDER = Color.fromRGBA(255, 255, 255, 32);
    static final Color CARD = Color.fromRGBA(255, 255, 255, 24);
    static final Color CARD_HOVER = Color.fromRGBA(255, 255, 255, 44);
    static final Color FIELD = Color.fromRGBA(255, 255, 255, 15);
    static final Color TRACK = Color.fromRGBA(255, 255, 255, 44);
    static final Color WARNING = Color.fromRGB(240, 173, 78);
    /** Off-brand red for "disabled" feedback. */
    public static final Color OFF = Color.fromRGB(224, 85, 99);
    private static final Color GLASS_BASE = Color.fromRGB(13, 17, 26);
    /** Vanilla {@code GameRenderer.MAX_BLUR_RADIUS}. */
    private static final int BLUR_MAX = 10;

    // Editable globals.
    private static ClientConfig.GuiThemePreset preset = ClientConfig.GuiThemePreset.EMERALD;
    private static Color accent = GuiThemePalette.of(preset).active();
    private static float glassOpacity = ClientConfig.UiPreferences.DEFAULT.glassOpacity();
    private static float cornerRadius = ClientConfig.UiPreferences.DEFAULT.cornerRadius();

    private GlassTheme() {
    }

    static void load(final ClientConfig.UiPreferences preferences) {
        preset = preferences.guiTheme();
        accent = GuiThemePalette.of(preset).active();
        glassOpacity = preferences.glassOpacity();
        cornerRadius = preferences.cornerRadius();
        glassBlur(preferences.glassBlur());
    }

    /**
     * The "Blur" control drives vanilla's menu-background blur strength (0..{@value #BLUR_MAX}); our
     * glass capture samples that blurred frame, so stronger vanilla blur means denser frosting — with
     * no extra render passes to crash on.
     */
    static float glassBlur() {
        Minecraft client = Minecraft.getInstance();
        return client == null || client.options == null ? 0F : client.options.getMenuBackgroundBlurriness();
    }

    static void glassBlur(final float value) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        client.options.menuBackgroundBlurriness().set(Math.max(0, Math.min(BLUR_MAX, Math.round(value))));
    }

    static ClientConfig.GuiThemePreset preset() {
        return preset;
    }

    static void preset(final ClientConfig.GuiThemePreset value) {
        preset = value == null ? ClientConfig.GuiThemePreset.EMERALD : value;
        accent = GuiThemePalette.of(preset).active();
    }

    public static Color accent() {
        return accent;
    }

    static Color accentSoft() {
        return accent.multiplyAlpha(0.42F);
    }

    static float glassOpacity() {
        return glassOpacity;
    }

    static void glassOpacity(final float value) {
        glassOpacity = clamp(value, 0.15F, 0.95F);
    }

    public static float cornerRadius() {
        return cornerRadius;
    }

    static void cornerRadius(final float value) {
        cornerRadius = clamp(value, 0F, 24F);
    }

    /** Tint of primary glass panels; alpha is the mix strength over the blurred scene. */
    public static Color glass() {
        return GLASS_BASE.withAlpha(Math.round(255F * glassOpacity));
    }

    /** Slightly denser glass for the inspector and drawers, keeping text readable. */
    static Color glassDeep() {
        return GLASS_BASE.withAlpha(Math.round(255F * clamp(glassOpacity + 0.14F, 0F, 0.97F)));
    }

    private static float clamp(final float value, final float min, final float max) {
        return Math.min(max, Math.max(min, value));
    }
}
