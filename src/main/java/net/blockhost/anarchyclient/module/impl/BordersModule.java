package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public final class BordersModule extends Module {

    static final int CHUNK_SIZE = 16;
    static final int MAP_SIZE = 128;
    /** Vanilla maps are centered on multiples of 128, so their edges sit 64 blocks off the grid. */
    static final int MAP_OFFSET = -64;

    private static final WorldLineRenderer.Color CHUNK_COLOR = new WorldLineRenderer.Color(255, 220, 60, 200);
    private static final WorldLineRenderer.Color MAP_COLOR = new WorldLineRenderer.Color(60, 200, 255, 220);

    private final BooleanSetting chunks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chunks")
            .name("Chunks")
            .defaultValue(true)
            .description("Outlines the chunk you stand in.")
            .build()));
    private final BooleanSetting maps = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("maps")
            .name("Maps")
            .defaultValue(true)
            .description("Outlines the 128 by 128 area that one zoomed-in map covers, for map art.")
            .build()));
    private final NumberSetting height = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("height")
            .name("Height")
            .defaultValue(16.0)
            .min(1.0)
            .max(128.0)
            .step(1.0)
            .description("How far the corner lines reach above and below you.")
            .build()));

    public BordersModule() {
        super(
                "borders",
                "Borders",
                ModuleCategory.RENDER,
                "Draws the edges of the chunk you stand in and of the area one map covers, so you can line up builds and map art.");
    }

    @Override
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        // Interpolated, like the camera the lines are drawn relative to; tick positions jittered at high FPS.
        Vec3 position = client.player.getPosition(client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        double x = position.x;
        double y = position.y;
        double z = position.z;
        double reach = this.height.value();
        if (this.chunks.value()) {
            drawArea(
                    matrices,
                    submits,
                    camera,
                    cellStart(x, CHUNK_SIZE, 0),
                    cellStart(z, CHUNK_SIZE, 0),
                    CHUNK_SIZE,
                    y,
                    reach,
                    CHUNK_COLOR);
        }
        if (this.maps.value()) {
            drawArea(
                    matrices,
                    submits,
                    camera,
                    cellStart(x, MAP_SIZE, MAP_OFFSET),
                    cellStart(z, MAP_SIZE, MAP_OFFSET),
                    MAP_SIZE,
                    y,
                    reach,
                    MAP_COLOR);
        }
    }

    /** Returns the lowest coordinate of the grid cell that contains {@code coordinate}. */
    static int cellStart(final double coordinate, final int size, final int offset) {
        return Math.floorDiv((int) Math.floor(coordinate) - offset, size) * size + offset;
    }

    private static void drawArea(
            final PoseStack matrices,
            final SubmitNodeCollector submits,
            final Vec3 camera,
            final int minX,
            final int minZ,
            final int size,
            final double y,
            final double reach,
            final WorldLineRenderer.Color color) {
        double maxX = minX + size;
        double maxZ = minZ + size;
        double[][] corners = {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
        for (int index = 0; index < corners.length; index++) {
            double[] corner = corners[index];
            double[] next = corners[(index + 1) % corners.length];
            line(matrices, submits, camera, new Vec3(corner[0], y, corner[1]), new Vec3(next[0], y, next[1]), color);
            line(
                    matrices,
                    submits,
                    camera,
                    new Vec3(corner[0], y - reach, corner[1]),
                    new Vec3(corner[0], y + reach, corner[1]),
                    color);
        }
    }

    private static void line(
            final PoseStack matrices,
            final SubmitNodeCollector submits,
            final Vec3 camera,
            final Vec3 start,
            final Vec3 end,
            final WorldLineRenderer.Color color) {
        WorldLineRenderer.lineNoDepth(matrices, submits, start.subtract(camera), end.subtract(camera), color);
    }
}
