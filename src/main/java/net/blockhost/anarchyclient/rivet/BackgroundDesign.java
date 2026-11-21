package net.blockhost.anarchyclient.rivet;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Animated backdrop applied to the client menu panels. Each design maps to a render pipeline, except
 * {@link #NONE} which leaves the panels as a plain dark fill.
 */
public enum BackgroundDesign {
    NONE("None", null),
    MATRIX("Matrix", AnarchyClientRenderPipelines.MATRIX_PANEL),
    CAUSTICS("Caustics", AnarchyClientRenderPipelines.CAUSTICS_PANEL),
    RETRO_GRID("Retro Grid", AnarchyClientRenderPipelines.RETRO_GRID_PANEL),
    FIRE("Fire", AnarchyClientRenderPipelines.FIRE_PANEL),
    DEEP("Deep", AnarchyClientRenderPipelines.DEEP_PANEL),
    SIMPLEX_FLOW("Simplex Flow", AnarchyClientRenderPipelines.SIMPLEX_FLOW_PANEL);

    private final String displayName;

    @SuppressWarnings("ImmutableEnumChecker") // Render pipelines are not changed after creation.
    private final @Nullable RenderPipeline pipeline;

    BackgroundDesign(final String displayName, final @Nullable RenderPipeline pipeline) {
        this.displayName = displayName;
        this.pipeline = pipeline;
    }

    public String displayName() {
        return this.displayName;
    }

    public @Nullable RenderPipeline pipeline() {
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
