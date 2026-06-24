package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;

import java.util.List;
import java.util.stream.Stream;

/**
 * Animated backdrop applied to the client menu panels. Each design maps to a render pipeline, except
 * {@link #NONE} which leaves the panels as a plain dark fill.
 */
public enum BackgroundDesign {

    NONE("None", null),
    MATRIX("Matrix", AnarchyClientRenderPipelines.MATRIX_PANEL),
    AURORA("Aurora", AnarchyClientRenderPipelines.AURORA_PANEL),
    GRID("Grid", AnarchyClientRenderPipelines.GRID_PANEL),
    EMBER("Ember", AnarchyClientRenderPipelines.EMBER_PANEL);

    private final String displayName;
    private final RenderPipeline pipeline;

    BackgroundDesign(final String displayName, final RenderPipeline pipeline) {
        this.displayName = displayName;
        this.pipeline = pipeline;
    }

    public String displayName() {
        return this.displayName;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public static List<String> displayNames() {
        return Stream.of(values()).map(BackgroundDesign::displayName).toList();
    }

    public static BackgroundDesign fromDisplayName(final String displayName) {
        for (BackgroundDesign design : values()) {
            if (design.displayName.equals(displayName)) {
                return design;
            }
        }
        return NONE;
    }
}
