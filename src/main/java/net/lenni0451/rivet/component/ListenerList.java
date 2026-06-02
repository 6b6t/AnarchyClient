package net.lenni0451.rivet.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ListenerList<L> {

    private final List<L> listeners = new ArrayList<>(0);

    public ListenerList<L> add(final L listener) {
        this.listeners.add(listener);
        return this;
    }

    public ListenerList<L> remove(final L listener) {
        this.listeners.remove(listener);
        return this;
    }

    public boolean call(final Predicate<L> listenerInvoker) {
        return this.call(listenerInvoker, () -> false);
    }

    public boolean call(final Predicate<L> listenerInvoker, final BooleanSupplier task) {
        for (L listener : this.listeners) {
            boolean value = listenerInvoker.test(listener);
            if (value) return true;
        }
        return task.getAsBoolean();
    }

    public void callVoid(final Consumer<L> listenerInvoker) {
        this.callVoid(listenerInvoker, () -> {});
    }

    public void callVoid(final Consumer<L> listenerInvoker, final Runnable task) {
        for (L listener : this.listeners) {
            listenerInvoker.accept(listener);
        }
        task.run();
    }

}
