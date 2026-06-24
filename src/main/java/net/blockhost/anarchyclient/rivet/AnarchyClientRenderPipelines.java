package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class AnarchyClientRenderPipelines {

    public static final RenderPipeline MATRIX_PANEL = panelPipeline("matrix_panel");
    public static final RenderPipeline AURORA_PANEL = panelPipeline("aurora_panel");
    public static final RenderPipeline GRID_PANEL = panelPipeline("grid_panel");
    public static final RenderPipeline EMBER_PANEL = panelPipeline("ember_panel");

    private AnarchyClientRenderPipelines() {
    }

    private static RenderPipeline panelPipeline(final String name) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "pipeline/" + name))
                .withVertexShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/panel"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/" + name))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .build());
    }

    public static void initialize() {
        // Loads the static pipelines during client initialization.
    }
}
