package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;

import java.util.List;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TRACK;

final class ColorStrip extends Component implements LayoutDebugLabel {

    private static final List<Float> FRACTIONS = List.of(0.18F, 0.18F, 0.18F, 0.18F, 0.14F, 0.14F);

    ColorStrip() {
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        float x = PADDING;
        float width = Math.max(0F, size.width() - PADDING * 2F);
        renderer.fillRect(x, 0F, width, size.height(), TRACK);
        float offset = 0F;
        List<Color> colors = this.colors();
        for (int index = 0; index < colors.size(); index++) {
            float segmentWidth = width * FRACTIONS.get(index);
            renderer.fillRect(x + offset, 0F, segmentWidth, size.height(), colors.get(index));
            offset += segmentWidth;
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 2F);
    }

    @Override
    public String layoutDebugLabel() {
        return "theme";
    }

    private List<Color> colors() {
        return List.of(
                Color.fromRGB(245, 56, 70),
                Color.fromRGB(245, 132, 35),
                Color.fromRGB(245, 204, 60),
                GlassTheme.accent(),
                Color.fromRGB(70, 116, 240),
                Color.fromRGB(227, 70, 150)
        );
    }
}