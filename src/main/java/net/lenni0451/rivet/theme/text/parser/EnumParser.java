package net.lenni0451.rivet.theme.text.parser;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
@RequiredArgsConstructor
public final class EnumParser<E extends Enum<E>> implements Parser<E> {

    private final E[] values;

    @Nullable
    @Override
    public E parse(final String s) {
        for (E value : this.values) {
            if (value.name().equalsIgnoreCase(s)) {
                return value;
            }
        }
        return null;
    }

}
