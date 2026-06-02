package net.lenni0451.rivet.text;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ParserException extends Exception {

    public ParserException(final String message) {
        super(Objects.requireNonNull(message));
    }

    public ParserException(final String message, final Throwable cause) {
        super(Objects.requireNonNull(message), Objects.requireNonNull(cause));
    }

}
