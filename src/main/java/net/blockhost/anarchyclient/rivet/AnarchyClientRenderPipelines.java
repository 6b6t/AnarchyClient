package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class AnarchyClientRenderPipelines {

    public static final RenderPipeline GLASS_PANEL = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "pipeline/glass_panel"))
                    .withVertexShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/panel"))
                    .withFragmentShader(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "core/glass_panel"))
                    .build()
    );
    public static final RenderPipeline SDF_FILL = panelPipeline("sdf_fill");
    public static final RenderPipeline SDF_SHADOW = panelPipeline("sdf_shadow");
    public static final RenderPipeline MATRIX_PANEL = panelPipeline("matrix_panel");
    public static final RenderPipeline CAUSTICS_PANEL = panelPipeline("caustics_panel");
    public static final RenderPipeline RETRO_GRID_PANEL = panelPipeline("retro_grid_panel");
    public static final RenderPipeline FIRE_PANEL = panelPipeline("fire_panel");
    public static final RenderPipeline DEEP_PANEL = panelPipeline("deep_panel");
    public static final RenderPipeline SIMPLEX_FLOW_PANEL = panelPipeline("simplex_flow_panel");
    private static final DepthStencilState NO_DEPTH = new DepthStencilState(CompareOp.ALWAYS_PASS, false);
    private static final RenderPipeline LINES_NO_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "pipeline/lines_no_depth"))
                    .withDepthStencilState(NO_DEPTH)
                    .build()
    );
    private static final RenderPipeline QUADS_NO_DEPTH_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "pipeline/quads_no_depth"))
                    .withDepthStencilState(NO_DEPTH)
                    .build()
    );
    public static final RenderType LINES_NO_DEPTH = RenderType.create(
            "anarchyclient:lines_no_depth",
            RenderSetup.builder(LINES_NO_DEPTH_PIPELINE).createRenderSetup()
    );
    public static final RenderType QUADS_NO_DEPTH = RenderType.create(
            "anarchyclient:quads_no_depth",
            RenderSetup.builder(QUADS_NO_DEPTH_PIPELINE).createRenderSetup()
    );

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
