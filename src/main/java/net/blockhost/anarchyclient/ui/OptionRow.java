package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.OPTION_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

/**
 * Generic clickable option row: label, optional live value text, optional toggle, optional chevron.
 */
final class OptionRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final String text;
    private final Supplier<String> value;
    private final BooleanSupplier checked;
    private final Runnable action;
    private final Runnable afterAction;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label;
    private final TextNode valueLabel;
    private final IconNode arrow;
    private final ToggleSwitch toggle;

    OptionRow(
            final ModulePanel panel,
            final String text,
            final boolean arrow,
            final BooleanSupplier checked,
            final Runnable action,
            final Runnable afterAction) {
        this(panel, text, arrow, checked, () -> "", action, afterAction);
    }

    OptionRow(
            final ModulePanel panel,
            final String text,
            final boolean arrow,
            final BooleanSupplier checked,
            final Supplier<String> value,
            final Runnable action,
            final Runnable afterAction) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.text = text;
        this.value = value;
        this.checked = checked;
        this.action = action;
        this.afterAction = afterAction;
        this.label = textNode(text, () -> this.checked == null || this.checked() ? TEXT : MUTED);
        this.valueLabel = textNode(this.value, GlassTheme::accent)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.arrow = arrow ? iconNode("chevron-right", FAINT) : null;
        this.toggle = checked == null ? null : new ToggleSwitch(this::checked, this::runAction);
        this.fixedSize(-1, OPTION_ROW_HEIGHT);
        place(
                this.divider,
                cell(
                        0,
                        1,
                        3,
                        1,
                        0F,
                        0F,
                        GridAnchor.BOTTOM,
                        GridFill.HORIZONTAL,
                        padding(PADDING, 0F, PADDING, 0F),
                        null,
                        1F));
        place(
                this.label,
                cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(PADDING, 0F, 8F, 0F), null, null));
        place(
                this.valueLabel,
                cell(1, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(0F, 0F, 4F, 0F), 86F, null));
        this.addChild(this.divider);
        this.addChild(this.label);
        this.addChild(this.valueLabel);
        if (this.arrow != null) {
            place(
                    this.arrow,
                    cell(
                            2,
                            0,
                            1,
                            1,
                            0F,
                            1F,
                            GridAnchor.CENTER,
                            GridFill.NONE,
                            padding(0F, 0F, 8F, 0F),
                            ICON_BOX,
                            ICON_BOX));
            this.addChild(this.arrow);
        }
        if (this.toggle != null) {
            place(
                    this.toggle,
                    cell(
                            2,
                            0,
                            1,
                            1,
                            0F,
                            1F,
                            GridAnchor.CENTER,
                            GridFill.NONE,
                            padding(0F, 0F, 14F, 0F),
                            SWITCH_WIDTH,
                            SWITCH_HEIGHT));
            this.addChild(this.toggle);
        }
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (super.onComponentMouseDown(event, size)) {
            return true;
        }
        if (event.button() == MouseButton.LEFT) {
            this.runAction();
            return true;
        }
        return false;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), OPTION_ROW_HEIGHT);
    }

    @Override
    public String layoutDebugLabel() {
        return this.text;
    }

    private boolean checked() {
        return this.checked != null && this.checked.getAsBoolean();
    }

    private void runAction() {
        this.action.run();
        this.afterAction.run();
        this.panel.requestFrame();
    }
}
