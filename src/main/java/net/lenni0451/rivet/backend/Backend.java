package net.lenni0451.rivet.backend;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.text.model.TextBlock;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;

import javax.annotation.Nullable;

public interface Backend {

    float getTextHeight();

    ShapedText shapeText(final String text, final Color color);

    default ShapedText shapeText(final TextSection section) {
        return this.shapeText(new TextLine(section));
    }

    ShapedText shapeText(final TextLine line);

    ShapedTextBlock shapeText(final TextBlock block);

    @Nullable
    String getClipboard();

    void setClipboard(final String clipboard);

    default boolean isKeyDown(final ModifierKey key) {
        return switch (key) {
            case SHIFT -> this.isKeyDown(Key.LEFT_SHIFT) || this.isKeyDown(Key.RIGHT_SHIFT);
            case CONTROL -> this.isKeyDown(Key.LEFT_CONTROL) || this.isKeyDown(Key.RIGHT_CONTROL);
            case ALT -> this.isKeyDown(Key.LEFT_ALT) || this.isKeyDown(Key.RIGHT_ALT);
            case SUPER -> this.isKeyDown(Key.LEFT_SUPER) || this.isKeyDown(Key.RIGHT_SUPER);
        };
    }

    boolean isKeyDown(final Key key);

}
