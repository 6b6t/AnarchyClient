package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.impl.DefaultDark;

final class AnarchyClientTheme extends DefaultDark {

    @Override
    protected void addValues(final Rivet rivet, final Values values) {
        super.addValues(rivet, values);

        values.put(Theme.TEXT_COLOR, Color.fromRGB(236, 232, 224));

        values.put(Theme.BUTTON_CORNER_RADIUS, 0F);
        values.put(Theme.BUTTON_OUTLINE_WIDTH, 1F);
        values.put(Theme.BUTTON_INACTIVE_COLOR, Color.fromRGBA(25, 25, 28, 164));
        values.put(Theme.BUTTON_INACTIVE_OUTLINE_COLOR, Color.fromRGBA(54, 54, 62, 120));
        values.put(Theme.BUTTON_ACTIVE_COLOR, Color.fromRGBA(34, 34, 38, 186));
        values.put(Theme.BUTTON_ACTIVE_OUTLINE_COLOR, Color.fromRGBA(82, 82, 92, 196));
        values.put(Theme.BUTTON_CLICK_COLOR, Color.fromRGBA(12, 12, 14, 120));
        values.put(Theme.BUTTON_CLICK_OUTLINE_COLOR, Color.fromRGB(0, 212, 170));
        values.put(Theme.BUTTON_INNER_PADDING, new Padding(6, 3, 6, 3));
        values.put(Theme.BUTTON_CLICK_ON, Button.ClickOn.UP);

        values.put(Theme.SLIDER_BAR_COLOR, Color.fromRGBA(50, 50, 56, 210));
        values.put(Theme.SLIDER_ACTIVE_BAR_COLOR, Color.fromRGB(0, 212, 170));
        values.put(Theme.SLIDER_THUMB_COLOR, Color.fromRGB(236, 232, 224));
        values.put(Theme.SLIDER_THUMB_CLICK_COLOR, Color.fromRGB(0, 212, 170));
        values.put(Theme.SLIDER_TICK_COLOR, Color.fromRGB(154, 150, 142));
        values.put(Theme.SLIDER_BAR_HEIGHT, 3F);
        values.put(Theme.SLIDER_THUMB_WIDTH, 8F);
        values.put(Theme.SLIDER_THUMB_HEIGHT, 8F);
        values.put(Theme.SLIDER_THUMB_SHAPE, Slider.ThumbShape.CIRCLE);
        values.put(Theme.SLIDER_TOOLTIP_BACKGROUND_COLOR, Color.fromRGBA(17, 17, 19, 238));
        values.put(Theme.SLIDER_TOOLTIP_TEXT_COLOR, Color.fromRGB(236, 232, 224));

        values.put(Theme.SCROLL_BAR_COLOR, Color.fromRGBA(154, 150, 142, 80));
        values.put(Theme.SCROLL_BAR_HOVER_COLOR, Color.fromRGBA(154, 150, 142, 130));
        values.put(Theme.SCROLL_BAR_CLICK_COLOR, Color.fromRGBA(0, 212, 170, 170));
        values.put(Theme.SCROLL_SPEED, 18F);
        values.put(Theme.SCROLL_BAR_TYPE, ScrollContainer.ScrollBarType.FLOATING);

        values.put(Theme.TEXT_FIELD_BACKGROUND_COLOR, Color.fromRGBA(12, 12, 14, 120));
        values.put(Theme.TEXT_FIELD_OUTLINE_COLOR, Color.fromRGBA(54, 54, 62, 160));
        values.put(Theme.TEXT_FIELD_FOCUSED_OUTLINE_COLOR, Color.fromRGB(0, 212, 170));
        values.put(Theme.TEXT_FIELD_SELECTION_COLOR, Color.fromRGBA(0, 212, 170, 90));
        values.put(Theme.TEXT_FIELD_CURSOR_COLOR, Color.fromRGB(236, 232, 224));
        values.put(Theme.TEXT_FIELD_CORNER_RADIUS, 0F);

        values.put(Theme.CHECKBOX_BACKGROUND_COLOR, Color.fromRGBA(12, 12, 14, 140));
        values.put(Theme.CHECKBOX_OUTLINE_COLOR, Color.fromRGBA(82, 82, 92, 196));
        values.put(Theme.CHECKBOX_CHECK_COLOR, Color.fromRGB(0, 212, 170));
        values.put(Theme.CHECKBOX_CORNER_RADIUS, 0F);

        values.put(Theme.SEPARATOR_COLOR, Color.fromRGBA(54, 54, 62, 120));
    }
}
