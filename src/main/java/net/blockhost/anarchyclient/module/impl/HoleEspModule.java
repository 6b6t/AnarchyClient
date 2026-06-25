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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class HoleEspModule extends Module {

    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(12.0)
            .min(4.0)
            .max(32.0)
            .step(2.0)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(6.0)
            .min(2.0)
            .max(16.0)
            .step(1.0)
            .build()));
    private final NumberSetting maxHoles = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_holes")
            .name("Max")
            .defaultValue(64.0)
            .min(8.0)
            .max(256.0)
            .step(8.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(85.0)
            .min(20.0)
            .max(180.0)
            .step(5.0)
            .build()));
    private final List<BlockPos> holes = new ArrayList<>();
    private int scanCooldown;

    public HoleEspModule() {
        super("hole_esp", "Hole ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.holes.clear();
            this.scanCooldown = 0;
            return;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return;
        }
        this.holes.clear();
        this.holes.addAll(scanHoles(client, this.radius.value().intValue(), this.vertical.value().intValue(),
                this.maxHoles.value().intValue()));
        this.scanCooldown = 10;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null || this.holes.isEmpty()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        int alpha = this.opacity.value().intValue();
        WorldLineRenderer.Color fill = new WorldLineRenderer.Color(80, 210, 150, alpha);
        WorldLineRenderer.Color outline = new WorldLineRenderer.Color(120, 255, 190, Math.min(255, alpha + 90));
        for (BlockPos hole : this.holes) {
            AABB box = new AABB(hole).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, fill);
            WorldLineRenderer.boxNoDepth(matrices, submits, box, outline);
        }
    }

    @Override
    protected void onDisable() {
        this.holes.clear();
        this.scanCooldown = 0;
    }

    private static List<BlockPos> scanHoles(final Minecraft client, final int radius, final int vertical, final int maxHoles) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || maxHoles <= 0) {
            return List.of();
        }
        BlockPos center = player.blockPosition();
        int minY = Math.max(client.level.getMinY(), center.getY() - vertical);
        int maxY = Math.min(client.level.getMinY() + client.level.getHeight() - 1, center.getY() + vertical);
        List<BlockPos> results = new ArrayList<>();
        for (int y = minY; y <= maxY && results.size() < maxHoles; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius && results.size() < maxHoles; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius && results.size() < maxHoles; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.isLoaded(pos) && isHole(client, pos)) {
                        results.add(pos.immutable());
                    }
                }
            }
        }
        return List.copyOf(results);
    }

    private static boolean isHole(final Minecraft client, final BlockPos pos) {
        return isHole(
                client.level.getBlockState(pos).isAir(),
                client.level.getBlockState(pos.above()).isAir(),
                safeBlock(client.level.getBlockState(pos.below())),
                safeBlock(client.level.getBlockState(pos.north())),
                safeBlock(client.level.getBlockState(pos.south())),
                safeBlock(client.level.getBlockState(pos.east())),
                safeBlock(client.level.getBlockState(pos.west()))
        );
    }

    static boolean isHole(final boolean feetEmpty, final boolean headEmpty, final boolean floorSafe,
                          final boolean northSafe, final boolean southSafe, final boolean eastSafe,
                          final boolean westSafe) {
        return feetEmpty && headEmpty && floorSafe && northSafe && southSafe && eastSafe && westSafe;
    }

    private static boolean safeBlock(final BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK
                || block == Blocks.OBSIDIAN
                || block == Blocks.CRYING_OBSIDIAN
                || block == Blocks.ENDER_CHEST
                || block == Blocks.RESPAWN_ANCHOR;
    }
}
