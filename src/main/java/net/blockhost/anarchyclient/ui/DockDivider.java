package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_DIVIDER;

final class DockDivider extends Component {

    DockDivider() {
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        float width = size.width() - 10F;
        renderer.fillRect(5F, size.height() / 2F, width, 1F, DIVIDER);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), DOCK_DIVIDER);
    }
}