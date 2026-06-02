package net.lenni0451.rivet.theme.impl;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.theme.Theme;

public class DefaultDark extends BaseTheme {

    @Override
    protected void addValues(final Rivet rivet, final Values values) {
        super.addValues(rivet, values);

        values.put(Theme.TEXT_COLOR, Color.WHITE);

        values.put(Theme.BUTTON_INACTIVE_COLOR, Color.fromRGB(45, 45, 48));
        values.put(Theme.BUTTON_INACTIVE_OUTLINE_COLOR, Color.fromRGB(65, 65, 70));
        values.put(Theme.BUTTON_ACTIVE_COLOR, Color.fromRGB(65, 65, 70));
        values.put(Theme.BUTTON_ACTIVE_OUTLINE_COLOR, Color.fromRGB(100, 100, 105));
        values.put(Theme.BUTTON_CLICK_COLOR, Color.fromRGB(55, 55, 60));
        values.put(Theme.BUTTON_CLICK_OUTLINE_COLOR, Color.fromRGB(110, 110, 115));

        values.put(Theme.SLIDER_BAR_COLOR, Color.fromRGB(65, 65, 70));
        values.put(Theme.SLIDER_ACTIVE_BAR_COLOR, Color.fromRGB(80, 80, 85));
        values.put(Theme.SLIDER_THUMB_COLOR, Color.fromRGB(120, 120, 125));
        values.put(Theme.SLIDER_THUMB_CLICK_COLOR, Color.fromRGB(100, 100, 105));
        values.put(Theme.SLIDER_TICK_COLOR, Color.fromRGB(160, 160, 165));
        values.put(Theme.SLIDER_TOOLTIP_BACKGROUND_COLOR, Color.fromRGB(45, 45, 48));
        values.put(Theme.SLIDER_TOOLTIP_TEXT_COLOR, Color.WHITE);

        values.put(Theme.SCROLL_BAR_COLOR, Color.fromRGBA(120, 120, 125, 100));
        values.put(Theme.SCROLL_BAR_HOVER_COLOR, Color.fromRGBA(140, 140, 145, 150));
        values.put(Theme.SCROLL_BAR_CLICK_COLOR, Color.fromRGBA(100, 100, 105, 200));
        values.put(Theme.SCROLL_BAR_OUTLINE_COLOR, Color.fromRGB(65, 65, 70));
        values.put(Theme.SCROLL_RAIL_COLOR, Color.fromRGB(37, 37, 38));
        values.put(Theme.SCROLL_RAIL_OUTLINE_COLOR, Color.fromRGB(51, 51, 52));

        values.put(Theme.TEXT_FIELD_BACKGROUND_COLOR, Color.fromRGB(30, 30, 30));
        values.put(Theme.TEXT_FIELD_OUTLINE_COLOR, Color.GRAY);
        values.put(Theme.TEXT_FIELD_FOCUSED_OUTLINE_COLOR, Color.WHITE);
        values.put(Theme.TEXT_FIELD_SELECTION_COLOR, Color.fromRGBA(100, 100, 255, 100));
        values.put(Theme.TEXT_FIELD_CURSOR_COLOR, Color.WHITE);

        values.put(Theme.CHECKBOX_BACKGROUND_COLOR, Color.fromRGB(30, 30, 30));
        values.put(Theme.CHECKBOX_OUTLINE_COLOR, Color.GRAY);
        values.put(Theme.CHECKBOX_CHECK_COLOR, Color.WHITE);

        values.put(Theme.COLOR_PICKER_OUTLINE_COLOR, Color.GRAY);

        values.put(Theme.COMBOBOX_ARROW_COLOR, Color.WHITE);

        values.put(Theme.SEPARATOR_COLOR, Color.fromRGB(65, 65, 70));
    }

}
