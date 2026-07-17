package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.input.mouse.ClickOn;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.theme.Theme;

final class AnarchyClientTheme extends Theme {

    private final GuiThemePalette palette;

    AnarchyClientTheme() {
        this(ClientConfig.GuiThemePreset.EMERALD);
    }

    AnarchyClientTheme(final ClientConfig.GuiThemePreset preset) {
        this.palette = GuiThemePalette.of(preset);
    }

    @Override
    protected void addValues(final Rivet rivet, final Values values) {
        Color active = this.palette.active();
        values.put(Theme.TEXT_COLOR, Color.fromRGBA(255, 255, 255, 235));

        values.put(Theme.BUTTON_CORNER_RADIUS, 8F);
        values.put(Theme.BUTTON_OUTLINE_WIDTH, 1F);
        values.put(Theme.BUTTON_INACTIVE_COLOR, Color.fromRGBA(255, 255, 255, 22));
        values.put(Theme.BUTTON_INACTIVE_OUTLINE_COLOR, Color.fromRGBA(255, 255, 255, 34));
        values.put(Theme.BUTTON_ACTIVE_COLOR, Color.fromRGBA(255, 255, 255, 40));
        values.put(Theme.BUTTON_ACTIVE_OUTLINE_COLOR, Color.fromRGBA(255, 255, 255, 60));
        values.put(Theme.BUTTON_CLICK_COLOR, Color.fromRGBA(255, 255, 255, 14));
        values.put(Theme.BUTTON_CLICK_OUTLINE_COLOR, active);
        values.put(Theme.BUTTON_INNER_PADDING, new Padding(6, 3, 6, 3));
        values.put(Theme.BUTTON_CLICK_ON, ClickOn.UP);

        values.put(Theme.SLIDER_BAR_COLOR, Color.fromRGBA(255, 255, 255, 40));
        values.put(Theme.SLIDER_ACTIVE_BAR_COLOR, active);
        values.put(Theme.SLIDER_THUMB_COLOR, Color.fromRGBA(255, 255, 255, 235));
        values.put(Theme.SLIDER_THUMB_CLICK_COLOR, active);
        values.put(Theme.SLIDER_TICK_COLOR, Color.fromRGBA(255, 255, 255, 90));
        values.put(Theme.SLIDER_BAR_HEIGHT, 3F);
        values.put(Theme.SLIDER_THUMB_WIDTH, 9F);
        values.put(Theme.SLIDER_THUMB_HEIGHT, 9F);
        values.put(Theme.SLIDER_THUMB_SHAPE, Slider.ThumbShape.CIRCLE);
        values.put(Theme.SLIDER_TOOLTIP_BACKGROUND_COLOR, Color.fromRGBA(14, 16, 24, 235));
        values.put(Theme.SLIDER_TOOLTIP_TEXT_COLOR, Color.fromRGBA(255, 255, 255, 235));

        values.put(Theme.SCROLL_BAR_COLOR, Color.fromRGBA(255, 255, 255, 46));
        values.put(Theme.SCROLL_BAR_HOVER_COLOR, Color.fromRGBA(255, 255, 255, 84));
        values.put(Theme.SCROLL_BAR_CLICK_COLOR, active.multiplyAlpha(0.72F));
        values.put(Theme.SCROLL_SPEED, 18F);
        values.put(Theme.SCROLL_BAR_TYPE, ScrollContainer.ScrollBarType.FLOATING);

        values.put(Theme.TEXT_FIELD_BACKGROUND_COLOR, Color.fromRGBA(255, 255, 255, 16));
        values.put(Theme.TEXT_FIELD_OUTLINE_COLOR, Color.fromRGBA(255, 255, 255, 36));
        values.put(Theme.TEXT_FIELD_FOCUSED_OUTLINE_COLOR, active);
        values.put(Theme.TEXT_FIELD_SELECTION_COLOR, this.palette.selection());
        values.put(Theme.TEXT_FIELD_CURSOR_COLOR, Color.fromRGBA(255, 255, 255, 235));
        values.put(Theme.TEXT_FIELD_CORNER_RADIUS, 8F);

        values.put(Theme.CHECKBOX_BACKGROUND_COLOR, Color.fromRGBA(255, 255, 255, 18));
        values.put(Theme.CHECKBOX_OUTLINE_COLOR, Color.fromRGBA(255, 255, 255, 56));
        values.put(Theme.CHECKBOX_CHECK_COLOR, active);
        values.put(Theme.CHECKBOX_CORNER_RADIUS, 4F);

        values.put(Theme.SEPARATOR_COLOR, Color.fromRGBA(255, 255, 255, 28));
    }
}
