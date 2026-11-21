package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawText;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textWidth;

import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

final class BrandBlock extends Component {

    BrandBlock() {
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        float clientWidth = textWidth(this, "client", GlassTheme.accent());
        float brandWidth = textWidth(this, "ANARCHY", TEXT);
        float x = size.width() - clientWidth - 6F - brandWidth;
        drawText(
                this,
                renderer,
                "ANARCHY",
                TEXT,
                x,
                size.height() / 2F,
                TextOrigin.Horizontal.LOGICAL_LEFT,
                TextOrigin.Vertical.LOGICAL_CENTER);
        drawText(
                this,
                renderer,
                "client",
                GlassTheme.accent(),
                x + brandWidth + 6F,
                size.height() / 2F,
                TextOrigin.Horizontal.LOGICAL_LEFT,
                TextOrigin.Vertical.LOGICAL_CENTER);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(148F, 16F);
    }
}
