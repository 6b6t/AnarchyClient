package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.FIELD;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.SEARCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textField;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import java.util.function.Consumer;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Size;

final class SearchInput extends Container implements LayoutDebugLabel {

    private static final float FIELD_TEXT_INSET = 6F;

    private final ModulePanel panel;

    private final String placeholder;
    private final Surface background = surface(FIELD)
            .outline(() -> this.fieldFocused() ? GlassTheme.accent() : Color.TRANSPARENT, 1F)
            .cornerRadius(9F);
    private final IconNode icon = iconNode("search", () -> this.fieldFocused() ? MUTED : FAINT);
    private final TextNode placeholderLabel;
    private final TextField field = textField("");
    private Consumer<String> changeListener = ignored -> {};

    SearchInput(final ModulePanel panel, final String placeholder) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.placeholder = placeholder;
        this.placeholderLabel = textNode(
                () -> this.field.text().isEmpty() && !this.fieldFocused() ? this.placeholder : "", () -> FAINT);
        this.field.backgroundColor().set(Color.fromRGBA(0, 0, 0, 0));
        this.field.outlineColor().set(Color.fromRGBA(0, 0, 0, 0));
        this.field.focusedOutlineColor().set(Color.fromRGBA(0, 0, 0, 0));
        this.field.innerPadding().set(new Padding(FIELD_TEXT_INSET, 0, FIELD_TEXT_INSET, 0));
        this.field.valueChangeListener().add(value -> this.changeListener.accept(value));
        place(this.background, fillCell(0, 0, 2, 1));
        place(
                this.icon,
                cell(
                        0,
                        0,
                        1,
                        1,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(8F, 0F, 0F, 0F),
                        ICON_BOX,
                        ICON_BOX));
        // The placeholder's left inset matches the field's inner text inset so the caret and
        // the placeholder text sit in exactly the same spot.
        place(
                this.placeholderLabel,
                cell(
                        1,
                        0,
                        1,
                        1,
                        1F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.HORIZONTAL,
                        padding(FIELD_TEXT_INSET, 0F, 8F, 0F),
                        null,
                        18F));
        place(
                this.field,
                cell(1, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.HORIZONTAL, padding(0F, 0F, 4F, 0F), null, 18F));
        this.addChild(this.background);
        this.addChild(this.icon);
        this.addChild(this.placeholderLabel);
        this.addChild(this.field);
    }

    void onChange(final Consumer<String> listener) {
        this.changeListener = listener;
    }

    void text(final String value) {
        this.field.text(value);
        this.changeListener.accept(value);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), SEARCH_HEIGHT);
    }

    @Override
    public String layoutDebugLabel() {
        return this.field.text().isBlank() ? "empty" : this.field.text();
    }

    private boolean fieldFocused() {
        return this.field.rivet() != null && this.field.rivet().focusedComponent() == this.field;
    }
}
