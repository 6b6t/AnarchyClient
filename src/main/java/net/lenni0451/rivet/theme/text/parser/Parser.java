package net.lenni0451.rivet.theme.text.parser;

import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

@ApiStatus.Internal
public interface Parser<T> {

    @Nullable
    T parse(final String s);

}
