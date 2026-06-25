package net.blockhost.anarchyclient.render;

import net.blockhost.anarchyclient.module.impl.WorldLineRenderer;

public record MarkerStyle(WorldLineRenderer.Color outline, WorldLineRenderer.Color fill) {

    public static final MarkerStyle CYAN = new MarkerStyle(
            new WorldLineRenderer.Color(95, 205, 255, 220),
            new WorldLineRenderer.Color(95, 205, 255, 35)
    );

    public boolean hasFill() {
        return this.fill != null && this.fill.alpha() > 0;
    }
}
