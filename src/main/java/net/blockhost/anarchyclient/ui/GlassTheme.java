package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.rivet.GlassBackdrop;
import net.lenni0451.commons.color.Color;

/**
 * Global design tokens for the liquid glass menu. Every component reads these live, so edits made in
 * the Theme tab restyle the whole UI immediately. The editable values are persisted through
 * {@link ClientConfig.UiPreferences}.
 */
final class GlassTheme {

    // Fixed tokens.
    static final Color TEXT = Color.fromRGBA(255, 255, 255, 236);
    static final Color MUTED = Color.fromRGBA(255, 255, 255, 152);
    static final Color FAINT = Color.fromRGBA(255, 255, 255, 96);
    static final Color DIVIDER = Color.fromRGBA(255, 255, 255, 26);
    static final Color CARD = Color.fromRGBA(255, 255, 255, 14);
    static final Color CARD_HOVER = Color.fromRGBA(255, 255, 255, 32);
    static final Color FIELD = Color.fromRGBA(255, 255, 255, 15);
    static final Color TRACK = Color.fromRGBA(255, 255, 255, 44);
    static final Color WARNING = Color.fromRGB(240, 173, 78);
    private static final Color GLASS_BASE = Color.fromRGB(13, 17, 26);

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
        GlassBackdrop.extraBlurPasses(Math.round(preferences.glassBlur()));
    }

    static float glassBlur() {
        return GlassBackdrop.extraBlurPasses();
    }

    static void glassBlur(final float passes) {
        GlassBackdrop.extraBlurPasses(Math.round(passes));
    }

    static ClientConfig.GuiThemePreset preset() {
        return preset;
    }

    static void preset(final ClientConfig.GuiThemePreset value) {
        preset = value == null ? ClientConfig.GuiThemePreset.EMERALD : value;
        accent = GuiThemePalette.of(preset).active();
    }

    static Color accent() {
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

    static float cornerRadius() {
        return cornerRadius;
    }

    static void cornerRadius(final float value) {
        cornerRadius = clamp(value, 0F, 24F);
    }

    /** Tint of primary glass panels; alpha is the mix strength over the blurred scene. */
    static Color glass() {
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
