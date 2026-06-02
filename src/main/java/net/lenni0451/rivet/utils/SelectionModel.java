package net.lenni0451.rivet.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.input.keyboard.ModifierKey;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Accessors(fluent = true, chain = true, makeFinal = true)
public final class SelectionModel<E> {

    private final Supplier<List<E>> dataSupplier;
    private final Set<E> selectedItems = Collections.newSetFromMap(new IdentityHashMap<>());
    @Getter
    private final ListenerList<Runnable> changeListeners = new ListenerList<>();
    private E lastSelected = null;

    public SelectionModel(final List<E> dataList) {
        this(() -> dataList);
    }

    public boolean isSelected(final E item) {
        return this.selectedItems.contains(item);
    }

    public Set<E> selected() {
        return Collections.unmodifiableSet(this.selectedItems);
    }

    public void clear() {
        if (!this.selectedItems.isEmpty()) {
            this.selectedItems.clear();
            this.lastSelected = null;
            this.fireChange();
        }
    }

    public void select(final E item, final Set<ModifierKey> modifiers) {
        this.select(item, modifiers.contains(ModifierKey.CONTROL), modifiers.contains(ModifierKey.SHIFT));
    }

    public void select(final E item, final boolean ctrl, final boolean shift) {
        List<E> currentData = this.dataSupplier.get();
        boolean changed = false;

        if (shift && this.lastSelected != null && currentData.contains(this.lastSelected) && currentData.contains(item)) {
            int start = currentData.indexOf(this.lastSelected);
            int end = currentData.indexOf(item);

            if (!ctrl) {
                if (!this.selectedItems.isEmpty()) changed = true;
                this.selectedItems.clear();
            }

            int min = Math.min(start, end);
            int max = Math.max(start, end);
            for (int i = min; i <= max; i++) {
                if (this.selectedItems.add(currentData.get(i))) {
                    changed = true;
                }
            }
        } else if (ctrl) {
            if (!this.selectedItems.remove(item)) {
                this.selectedItems.add(item);
            }
            this.lastSelected = item;
            changed = true;
        } else {
            if (this.selectedItems.size() != 1 || !this.selectedItems.contains(item)) {
                changed = true;
            }
            this.selectedItems.clear();
            this.selectedItems.add(item);
            this.lastSelected = item;
        }

        if (changed) {
            this.fireChange();
        }
    }

    private void fireChange() {
        this.changeListeners.callVoid(Runnable::run);
    }

}
