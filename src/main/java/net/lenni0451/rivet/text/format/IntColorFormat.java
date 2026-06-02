package net.lenni0451.rivet.text.format;

import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.text.ParserException;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Internal
@RequiredArgsConstructor
public final class IntColorFormat implements ColorFormat {

    private final String name;
    private final int channels;
    private final Function<int[], Color> parser;

    public boolean canParse(final String s) {
        return s.startsWith(this.name + "(") && s.endsWith(")");
    }

    public Color parse(final String s) throws ParserException {
        int[] values = new int[this.channels];
        String inner = s.substring(this.name.length() + 1, s.length() - 1);
        if (inner.startsWith("#")) {
            String hex = inner.substring(1);
            if (hex.length() != this.channels * 2) {
                throw new ParserException("Invalid " + this.name + " hex color length: " + s);
            }
            for (int i = 0; i < this.channels; i++) {
                values[i] = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
        } else {
            String[] parts = inner.split(",");
            if (parts.length != this.channels) {
                throw new ParserException("Invalid " + this.name + " color format: " + s);
            }
            for (int i = 0; i < this.channels; i++) {
                values[i] = Integer.parseInt(parts[i].trim());
            }
        }
        return this.parser.apply(values);
    }

}
