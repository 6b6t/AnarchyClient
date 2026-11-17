package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.CARD_GAP;

/**
 * Two-column card grid. Rivet's list layouts stack a single column, so this container places
 * cards itself and reports the resulting content height for the surrounding scroll container.
 */
final class CardGrid extends Container {

    private final ModulePanel panel;

    CardGrid(final ModulePanel panel) {
        super(AbsoluteLayout.INSTANCE);
        this.panel = panel;
    }

    float cardHeight() {
        return this.panel.compactRows ? 40F : 46F;
    }

    @Override
    public void computeLayout(final Size size) {
        float cardWidth = (size.width() - CARD_GAP) / 2F;
        float height = this.cardHeight();
        int index = 0;
        for (Component child : this.children()) {
            if (child instanceof EmptyState) {
                child.layoutOptions(new AbsoluteOptions(0F, 0F, size.width(), 64F));
                continue;
            }
            int column = index % 2;
            int row = index / 2;
            child.layoutOptions(new AbsoluteOptions(
                    column * (cardWidth + CARD_GAP),
                    row * (height + CARD_GAP),
                    cardWidth,
                    height
            ));
            index++;
        }
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        int cards = 0;
        boolean empty = false;
        for (Component child : this.children()) {
            if (child instanceof EmptyState) {
                empty = true;
            } else {
                cards++;
            }
        }
        if (empty && cards == 0) {
            return new Size(constraints.width(), 64F);
        }
        int rows = (cards + 1) / 2;
        float height = rows == 0 ? 0F : rows * this.cardHeight() + (rows - 1) * CARD_GAP;
        return new Size(constraints.width(), height);
    }
}