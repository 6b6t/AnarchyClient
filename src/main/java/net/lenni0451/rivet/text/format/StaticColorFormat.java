package net.lenni0451.rivet.text.format;

import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.color.Color;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@RequiredArgsConstructor
public final class StaticColorFormat implements ColorFormat {

    private final String name;
    private final Color color;

    @Override
    public boolean canParse(final String s) {
        return s.equalsIgnoreCase(this.name);
    }

    @Override
    public Color parse(final String s) {
        return this.color;
    }

}
