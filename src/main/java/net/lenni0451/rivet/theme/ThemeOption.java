package net.lenni0451.rivet.theme;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(fluent = true, chain = true, makeFinal = true)
public final class ThemeOption<T> {

    @Getter
    private final Component component;
    @Getter
    private final ThemeKey<T> key;
    @Getter
    private final ListenerList<Consumer<T>> changeListener = new ListenerList<>();
    @Nullable
    private Supplier<T> value;

    public ThemeOption(final Component component, final ThemeKey<T> key) {
        this.component = component;
        this.key = key;
    }

    public T value() {
        if (this.value != null) return this.value.get();
        Rivet rivet = this.component.rivet();
        if (rivet == null) {
            throw new IllegalStateException("Unable to get theme value for key " + this.key + " without a rivet instance");
        }
        return rivet.theme().get(this.key);
    }

    public ThemeOption<T> set(@Nullable final T value) {
        if (value == null) {
            this.value = null;
        } else {
            this.value = () -> value;
        }
        this.fireChangeListener();
        return this;
    }

    public ThemeOption<T> set(@Nullable final Supplier<T> valueSupplier) {
        this.value = valueSupplier;
        this.fireChangeListener();
        return this;
    }

    public ThemeOption<T> reset() {
        this.value = null;
        this.fireChangeListener();
        return this;
    }

    private void fireChangeListener() {
        T currentValue = null;
        try {
            currentValue = this.value();
        } catch (IllegalStateException ignored) {
        }
        final T finalValue = currentValue;
        this.changeListener.callVoid(c -> c.accept(finalValue));
    }

}
