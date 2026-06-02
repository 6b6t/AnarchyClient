package net.lenni0451.rivet.text.format;

import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.text.ParserException;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Internal
@RequiredArgsConstructor
public final class FloatColorFormat implements ColorFormat {

    private final String name;
    private final int channels;
    private final Function<float[], Color> parser;

    @Override
    public boolean canParse(final String s) {
        return s.startsWith(this.name + "(") && s.endsWith(")");
    }

    @Override
    public Color parse(final String s) throws ParserException {
        float[] values = new float[this.channels];
        String inner = s.substring(this.name.length() + 1, s.length() - 1);
        String[] parts = inner.split(",");
        if (parts.length != this.channels) {
            throw new ParserException("Invalid " + this.name + " color format: " + s);
        }
        for (int i = 0; i < this.channels; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return this.parser.apply(values);
    }

}
