package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.command.CommandPrefix;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

/**
 * The Theme tab: edits the {@link GlassTheme} global design tokens live.
 */
final class ThemePanel extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final GlassSurface background;
    private final TextNode title;
    private final Container rows;
    private final ScrollContainer scroll;

    ThemePanel(final ModulePanel panel) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.background = this.panel.glassPanel();
        this.title = textNode("Theme", TEXT);
        this.rows = new Container(new VerticalListLayout(0, true));
        this.scroll = new LayoutDebugScrollContainer(this.rows);
        this.panel.configureScroll(this.scroll);
        place(this.background, fillCell(0, 0, 1, 2));
        place(this.title, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING + 2F, 8F, PADDING, 0F), null, 30F));
        place(this.scroll, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 2F, 0F, PADDING), null, null));
        this.addChild(this.background);
        this.addChild(this.title);
        this.addChild(this.scroll);
    }

    void refresh() {
        this.rows.clearChildren();
        this.rows.addChild(new GroupHeader("Accent"));
        this.rows.addChild(new AccentRow(this.panel));
        this.rows.addChild(new ColorStrip());
        this.rows.addChild(new GroupHeader("Glass"));
        this.rows.addChild(new SliderRow(this.panel, "Glass opacity", "How dense the glass panels are.",
                15F, 95F, 1F, () -> GlassTheme.glassOpacity() * 100F,
                value -> GlassTheme.glassOpacity(value / 100F),
                () -> Math.round(GlassTheme.glassOpacity() * 100F) + "%"));
        this.rows.addChild(new SliderRow(this.panel, "Blur", "How strongly the panels blur the game behind them.",
                0F, 10F, 1F, GlassTheme::glassBlur,
                GlassTheme::glassBlur,
                () -> Integer.toString(Math.round(GlassTheme.glassBlur()))));
        this.rows.addChild(new OptionRow(this.panel, "Background design", true, null,
                () -> this.panel.backgroundDesign.displayName(),
                this.panel::cycleBackgroundDesign, this::refresh));
        this.rows.addChild(new GroupHeader("Layout"));
        this.rows.addChild(new OptionRow(this.panel, "Compact cards", false,
                () -> this.panel.compactRows,
                () -> {
                    this.panel.compactRows = !this.panel.compactRows;
                    this.panel.saveUiPreferences();
                    this.panel.refreshModuleList();
                }, this::refresh));
        this.rows.addChild(new OptionRow(this.panel, "Hover descriptions", false,
                () -> this.panel.showSummaries,
                () -> {
                    this.panel.showSummaries = !this.panel.showSummaries;
                    this.panel.saveUiPreferences();
                }, this::refresh));
        this.rows.addChild(new OptionRow(this.panel, "Wide inspector", false,
                () -> this.panel.wideInspector,
                () -> {
                    this.panel.wideInspector = !this.panel.wideInspector;
                    this.panel.saveUiPreferences();
                }, this::refresh));
        this.rows.addChild(new GroupHeader("Commands"));
        this.rows.addChild(new OptionRow(this.panel, "Command prefix", true, null,
                CommandPrefix::get,
                () -> {
                    CommandPrefix.cycle();
                    this.panel.config.save();
                }, this::refresh));
        this.rows.addChild(new OptionRow(this.panel, "Reset UI preferences", false, null,
                () -> "", this.panel::resetUiPreferences, this::refresh));
        this.panel.requestFrame();
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return "theme=" + GlassTheme.preset().key();
    }
}