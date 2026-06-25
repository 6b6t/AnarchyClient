package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.lenni0451.commons.color.Color;

record GuiThemePalette(Color active) {

    static GuiThemePalette of(final ClientConfig.GuiThemePreset preset) {
        return switch (preset) {
            case EMERALD -> new GuiThemePalette(Color.fromRGB(0, 186, 148));
            case AMBER -> new GuiThemePalette(Color.fromRGB(222, 148, 54));
            case ROSE -> new GuiThemePalette(Color.fromRGB(218, 82, 116));
            case CYAN -> new GuiThemePalette(Color.fromRGB(64, 165, 208));
        };
    }

    Color activeSoft() {
        return this.active.multiplyAlpha(0.39F);
    }

    Color selection() {
        return this.active.multiplyAlpha(0.4F);
    }
}
