package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class AnarchyClientRenderPipelines {

    public static final RenderPipeline MATRIX_PANEL = RenderPipelines.register(RenderPipeline.builder(
                    RenderPipelines.MATRICES_PROJECTION_SNIPPET,
                    RenderPipelines.GLOBALS_SNIPPET
            )
            .withLocation(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "pipeline/matrix_panel"))
            .withVertexShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/matrix_panel"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/matrix_panel"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
            .build());

    private AnarchyClientRenderPipelines() {
    }

    public static void initialize() {
        // Loads the static pipeline during client initialization.
    }
}
