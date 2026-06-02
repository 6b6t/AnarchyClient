package net.lenni0451.rivet.text.format;

import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.color.Color;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Internal
@RequiredArgsConstructor
public final class HexColorFormat implements ColorFormat {

    private final int channels;
    private final Function<int[], Color> parser;

    @Override
    public boolean canParse(final String s) {
        return s.startsWith("#") && s.length() == (1 + this.channels * 2);
    }

    @Override
    public Color parse(final String s) {
        int[] values = new int[this.channels];
        String hex = s.substring(1);
        for (int i = 0; i < this.channels; i++) {
            values[i] = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return this.parser.apply(values);
    }

}
