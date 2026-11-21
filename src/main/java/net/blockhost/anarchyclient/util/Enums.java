package net.blockhost.anarchyclient.util;

/** Helpers for enum constants. */
public final class Enums {

    private Enums() {}

    /** Returns the constant declared after {@code value}, and the first constant after the last one. */
    @SuppressWarnings("EnumOrdinal") // Cycling follows the declaration order on purpose.
    public static <E extends Enum<E>> E next(final E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }
}
