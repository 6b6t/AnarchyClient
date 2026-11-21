package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawIcon;

import java.util.function.Supplier;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;

final class IconNode extends Component implements LayoutDebugLabel {

    private final Supplier<String> icon;
    private final Supplier<Color> color;

    IconNode(final Supplier<String> icon, final Supplier<Color> color) {
        this.icon = icon;
        this.color = color;
        this.fixedSize(ICON_BOX, ICON_BOX);
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        float iconX = (size.width() - ICON_BOX) / 2F;
        float iconY = (size.height() - ICON_BOX) / 2F;
        renderer.translate(
                iconX,
                iconY,
                () -> drawIcon(renderer, this.icon.get(), ICON_BOX / 2F, ICON_BOX / 2F, this.color.get()));
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(ICON_BOX, ICON_BOX);
    }

    @Override
    public String layoutDebugLabel() {
        return this.icon.get();
    }
}
