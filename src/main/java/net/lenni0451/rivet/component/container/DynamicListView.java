package net.lenni0451.rivet.component.container;

import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.dragdrop.DropMarkerStrategy;
import net.lenni0451.rivet.layout.Layout;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DynamicListView<E> extends ReorderableContainer {

    private final Supplier<List<E>> listSupplier;
    private final Function<E, Component> componentFactory;
    private final IdentityHashMap<E, Component> componentCache = new IdentityHashMap<>();
    private final List<E> lastSeenList = new ArrayList<>();

    private final IdentityHashMap<Component, Integer> orderMap = new IdentityHashMap<>();
    private final Comparator<Component> orderComparator = Comparator.comparingInt(c -> this.orderMap.getOrDefault(c, 0));

    public DynamicListView(final Layout layout, final List<E> list, final Function<E, Component> componentFactory) {
        this(layout, null, obj -> false, () -> list, componentFactory);
    }

    public DynamicListView(final Layout layout, final Supplier<List<E>> listSupplier, final Function<E, Component> componentFactory) {
        this(layout, null, obj -> false, listSupplier, componentFactory);
    }

    public DynamicListView(final Layout layout, final DropMarkerStrategy strategy, final Predicate<Object> dropFilter, final List<E> list, final Function<E, Component> componentFactory) {
        this(layout, strategy, dropFilter, () -> list, componentFactory);
    }

    public DynamicListView(final Layout layout, final DropMarkerStrategy strategy, final Predicate<Object> dropFilter, final Supplier<List<E>> listSupplier, final Function<E, Component> componentFactory) {
        super(layout, strategy, dropFilter);
        this.listSupplier = listSupplier;
        this.componentFactory = componentFactory;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        List<E> currentList = this.listSupplier.get();
        if (!this.isSame(currentList, this.lastSeenList)) {
            this.requestLayoutRecalculation();
        }
        super.render(renderer, bounds);
    }

    @Override
    public void computeLayout(final Size size) {
        List<E> currentList = this.listSupplier.get();
        if (!this.isSame(currentList, this.lastSeenList)) {
            this.syncChildren(currentList);
        }
        super.computeLayout(size);
    }

    private boolean isSame(final List<E> a, final List<E> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) return false;
        }
        return true;
    }

    private void syncChildren(final List<E> currentList) {
        for (E item : this.lastSeenList) {
            if (!currentList.contains(item)) {
                Component comp = this.componentCache.remove(item);
                if (comp != null) {
                    this.removeChild(comp);
                }
            }
        }

        this.orderMap.clear();
        for (int i = 0; i < currentList.size(); i++) {
            E item = currentList.get(i);
            Component component = this.componentCache.computeIfAbsent(item, itm -> {
                Component c = this.componentFactory.apply(itm);
                this.addChild(c);
                return c;
            });
            this.orderMap.put(component, i);
        }
        this.sortChildren(this.orderComparator);

        this.lastSeenList.clear();
        this.lastSeenList.addAll(currentList);
    }

}
