package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class LightOverlayModule extends Module {

    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(16.0)
            .min(4.0)
            .max(48.0)
            .step(2.0)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(8.0)
            .min(2.0)
            .max(32.0)
            .step(1.0)
            .build()));
    private final NumberSetting lightLevel = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("light_level")
            .name("Light")
            .defaultValue(0.0)
            .min(0.0)
            .max(15.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxMarkers = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_markers")
            .name("Max")
            .defaultValue(256.0)
            .min(16.0)
            .max(1024.0)
            .step(16.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(180.0)
            .min(30.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final List<BlockPos> markers = new ArrayList<>();
    private int scanCooldown;

    public LightOverlayModule() {
        super("light_overlay", "Light Overlay", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.markers.clear();
            this.scanCooldown = 0;
            return;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return;
        }
        this.markers.clear();
        this.markers.addAll(scanMarkers(client, this.radius.value().intValue(), this.vertical.value().intValue(),
                this.lightLevel.value().intValue(), this.maxMarkers.value().intValue()));
        this.scanCooldown = 10;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null || this.markers.isEmpty()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color = new WorldLineRenderer.Color(255, 92, 92, this.opacity.value().intValue());
        for (BlockPos pos : this.markers) {
            renderMarker(matrices, submits, pos, camera, color);
        }
    }

    @Override
    protected void onDisable() {
        this.markers.clear();
        this.scanCooldown = 0;
    }

    static boolean shouldRenderSpawnMarker(final boolean feetAir, final boolean headAir, final boolean floorSturdy,
                                           final int rawBrightness, final int maxLightLevel) {
        return feetAir && headAir && floorSturdy && rawBrightness <= maxLightLevel;
    }

    private static List<BlockPos> scanMarkers(final Minecraft client, final int radius, final int vertical,
                                              final int maxLightLevel, final int maxMarkers) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || maxMarkers <= 0) {
            return List.of();
        }
        BlockPos center = player.blockPosition();
        int minY = Math.max(client.level.getMinY() + 1, center.getY() - vertical);
        int maxY = Math.min(client.level.getMinY() + client.level.getHeight() - 2, center.getY() + vertical);
        List<MarkerCandidate> candidates = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.isLoaded(pos) || !isSpawnMarker(client.level, pos, maxLightLevel)) {
                        continue;
                    }
                    candidates.add(new MarkerCandidate(pos.immutable(), center.distSqr(pos)));
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(MarkerCandidate::distanceSqr));
        return candidates.stream()
                .limit(maxMarkers)
                .map(MarkerCandidate::pos)
                .toList();
    }

    private static boolean isSpawnMarker(final LevelReader level, final BlockPos pos, final int maxLightLevel) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockPos floorPos = pos.below();
        BlockState floor = level.getBlockState(floorPos);
        return shouldRenderSpawnMarker(
                feet.isAir(),
                head.isAir(),
                floor.isFaceSturdy(level, floorPos, Direction.UP),
                level.getMaxLocalRawBrightness(pos),
                maxLightLevel
        );
    }

    private static void renderMarker(final PoseStack matrices, final SubmitNodeCollector submits, final BlockPos pos,
                                     final Vec3 camera, final WorldLineRenderer.Color color) {
        double y = pos.getY() + 0.03;
        Vec3 a = new Vec3(pos.getX() + 0.18, y, pos.getZ() + 0.18).subtract(camera);
        Vec3 b = new Vec3(pos.getX() + 0.82, y, pos.getZ() + 0.82).subtract(camera);
        Vec3 c = new Vec3(pos.getX() + 0.82, y, pos.getZ() + 0.18).subtract(camera);
        Vec3 d = new Vec3(pos.getX() + 0.18, y, pos.getZ() + 0.82).subtract(camera);
        WorldLineRenderer.lineNoDepth(matrices, submits, a, b, color);
        WorldLineRenderer.lineNoDepth(matrices, submits, c, d, color);
    }

    private record MarkerCandidate(BlockPos pos, double distanceSqr) {
    }
}
