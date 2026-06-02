package net.lenni0451.rivet.theme.text.parser;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.text.ParserException;
import net.lenni0451.rivet.text.format.ColorFormat;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
public final class ColorParser implements Parser<Color> {

    @Nullable
    @Override
    public Color parse(final String s) {
        for (ColorFormat format : ColorFormat.FORMATS) {
            try {
                if (format.canParse(s)) {
                    return format.parse(s);
                }
            } catch (ParserException e) {
                throw new IllegalArgumentException("Unable to parse '" + s + "': " + e.getMessage(), e);
            }
        }
        return null;
    }

}
