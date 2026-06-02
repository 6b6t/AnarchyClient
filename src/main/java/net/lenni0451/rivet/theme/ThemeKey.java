package net.lenni0451.rivet.theme;

public record ThemeKey<T>(String name, Class<T> type) {

    public T verifyAndCast(final Object o) {
        if (o == null) {
            throw new NullPointerException("Value for key '" + this.name + "' cannot be null");
        } else if (!this.type.isInstance(o)) {
            throw new ClassCastException("Cannot cast '" + o.getClass().getName() + "' to '" + this.type.getName() + "' for key '" + this.name + "'");
        }
        return (T) o;
    }

}
