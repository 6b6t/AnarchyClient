package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.lenni0451.commons.color.Color;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

/**
 * Layout sizes, colors, and fonts shared by the client menu components.
 */
final class PanelStyle {

    static final float DOCK_WIDTH = 46F;
    static final float DOCK_BUTTON = 30F;
    static final float DOCK_GAP = 2F;
    static final float DOCK_DIVIDER = 7F;
    static final float PADDING = 12F;
    static final float SEARCH_HEIGHT = 28F;
    static final float HEADER_HEIGHT = 36F;
    static final float CARD_GAP = 8F;
    static final float SETTING_ROW_HEIGHT = 46F;
    static final float NUMBER_ROW_HEIGHT = 64F;
    static final float TEXT_ROW_HEIGHT = 54F;
    // A text row without a description: the padded 24px field plus the divider.
    static final float TEXT_FIELD_ROW_HEIGHT = 39F;
    static final float OPTION_ROW_HEIGHT = 34F;
    // Height of the one-line description under a setting label. Rows without a description drop it.
    static final float DESCRIPTION_HEIGHT = 17F;
    static final float INSPECTOR_HEADER_HEIGHT = 96F;
    // The switch box is 2px taller than the painted track so the SDF anti-aliased edge and
    // half-pixel snapping can never clip the pill at the bottom.
    static final float SWITCH_WIDTH = 24F;
    static final float SWITCH_HEIGHT = 14F;
    static final float SWITCH_TRACK_HEIGHT = 12F;
    static final float ICON_BOX = 20F;
    static final float ICON_BUTTON_SIZE = 28F;
    // The Lucide TTF rasterizes as a 20px icon font in Minecraft. Keep the
    // component box at that size so Rivet's component scissor does not crop it,
    // and calibrate the font's y anchor once inside IconNode.
    static final float ICON_TEXT_Y_FROM_CENTER = 2F;

    static final Color TEXT = GlassTheme.TEXT;
    static final Color MUTED = GlassTheme.MUTED;
    static final Color FAINT = GlassTheme.FAINT;
    static final Color DIVIDER = GlassTheme.DIVIDER;
    static final Color CARD = GlassTheme.CARD;
    static final Color CARD_HOVER = GlassTheme.CARD_HOVER;
    static final Color FIELD = GlassTheme.FIELD;
    static final Color TRACK = GlassTheme.TRACK;
    static final Color WARNING = GlassTheme.WARNING;
    static final FontDescription LUCIDE_FONT =
            new FontDescription.Resource(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "lucide"));

    private PanelStyle() {}
}
