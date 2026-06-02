package net.lenni0451.rivet.theme.impl;

import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.theme.Theme;

public abstract class BaseTheme extends Theme {

    @Override
    protected void addValues(final Rivet rivet, final Values values) {
        float textHeight = rivet.backend().getTextHeight();

        values.put(Theme.BUTTON_CORNER_RADIUS, round(textHeight / 10F, 0));
        values.put(Theme.BUTTON_OUTLINE_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.BUTTON_ANIMATION_DURATION, 150);
        values.put(Theme.BUTTON_INNER_PADDING, new Padding(round(textHeight / 3F, 0), round(textHeight / 10F, 0), round(textHeight / 3F, 0), round(textHeight / 10F, 0)));
        values.put(Theme.BUTTON_CLICK_ON, Button.ClickOn.UP);

        values.put(Theme.SLIDER_BAR_HEIGHT, round(textHeight / 3F, 1));
        values.put(Theme.SLIDER_THUMB_WIDTH, round(textHeight / 3F, 1) * 2F);
        values.put(Theme.SLIDER_THUMB_HEIGHT, round(textHeight / 3F, 1) * 2F);
        values.put(Theme.SLIDER_BAR_CORNER_RADIUS, Float.MAX_VALUE);
        values.put(Theme.SLIDER_THUMB_CORNER_RADIUS, Float.MAX_VALUE);
        values.put(Theme.SLIDER_THUMB_ENCASED, false);
        values.put(Theme.SLIDER_THUMB_SHAPE, Slider.ThumbShape.CIRCLE);
        values.put(Theme.SLIDER_TOOLTIP_CORNER_RADIUS, round(textHeight / 10F, 0));
        values.put(Theme.SLIDER_TOOLTIP_TRIANGLE_SIZE, round(textHeight / 4F, 1));
        values.put(Theme.SLIDER_TOOLTIP_PADDING, new Padding(round(textHeight / 10F, 0), 0, round(textHeight / 10F, 0), 0));
        values.put(Theme.SLIDER_TOOLTIP_FORMAT, "%,f");

        values.put(Theme.SCROLL_BAR_WIDTH, round(textHeight / 7F, 1));
        values.put(Theme.SCROLL_BAR_CORNER_RADIUS, Float.MAX_VALUE);
        values.put(Theme.SCROLL_BAR_OUTLINE_WIDTH, 0F);
        values.put(Theme.SCROLL_SPEED, round(textHeight * 4, 1));
        values.put(Theme.SCROLL_SMOOTH, true);
        values.put(Theme.SCROLL_ANIMATION_DURATION, 100);
        values.put(Theme.SCROLL_NESTED_SCROLL_TIMEOUT, 150L);
        values.put(Theme.SCROLL_BAR_TYPE, ScrollContainer.ScrollBarType.FLOATING);
        values.put(Theme.SCROLL_RAIL_CLICK_JUMP, true);
        values.put(Theme.SCROLL_RAIL_OUTLINE_WIDTH, 0F);

        values.put(Theme.TEXT_FIELD_CURSOR_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.TEXT_FIELD_OUTLINE_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.TEXT_FIELD_CORNER_RADIUS, 0F);
        values.put(Theme.TEXT_FIELD_INNER_PADDING, new Padding(round(textHeight / 5F, 0), round(textHeight / 10F, 0), round(textHeight / 5F, 0), round(textHeight / 10F, 0)));

        values.put(Theme.CHECKBOX_CORNER_RADIUS, round(textHeight / 25F, 1));
        values.put(Theme.CHECKBOX_OUTLINE_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.CHECKBOX_CHECK_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.CHECKBOX_TEXT_GAP, 0F);
        values.put(Theme.CHECKBOX_ANIMATION_DURATION, 100);

        values.put(Theme.COLOR_PICKER_OUTLINE_WIDTH, round(textHeight / 25F, 1));
        values.put(Theme.COLOR_PICKER_PICKER_SIZE, round(textHeight * 8F, 1));
        values.put(Theme.COLOR_PICKER_SLIDER_WIDTH, round(textHeight, 1));
        values.put(Theme.COLOR_PICKER_GAP, round(textHeight / 3F, 1));
        values.put(Theme.COLOR_PICKER_SELECTOR_SIZE, round(textHeight / 8F, 1));

        values.put(Theme.COMBOBOX_ARROW_SIZE, round(textHeight / 2F, 1));
        values.put(Theme.COMBOBOX_MAX_POPUP_HEIGHT, round(textHeight * 10F, 1));

        values.put(Theme.SEPARATOR_THICKNESS, round(textHeight / 20F, 1));
    }

    private static float round(final float value, final float min) {
        return Math.max(Math.round(value), min);
    }

}
