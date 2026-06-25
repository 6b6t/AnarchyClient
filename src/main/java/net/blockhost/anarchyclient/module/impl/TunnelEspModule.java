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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class TunnelEspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(24.0)
            .min(8.0)
            .max(64.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks")
            .name("Max")
            .defaultValue(96.0)
            .min(16.0)
            .max(512.0)
            .step(16.0)
            .build()));
    private List<BlockPos> tunnels = List.of();
    private int cooldownTicks;

    public TunnelEspModule() {
        super("tunnel_esp", "Tunnel ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.tunnels = List.of();
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        this.cooldownTicks = 30;
        this.tunnels = scan(client, player.blockPosition(), this.range.value().intValue(), this.maxBlocks.value().intValue());
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (BlockPos pos : this.tunnels) {
            WorldLineRenderer.boxNoDepth(matrices, submits, new AABB(pos).move(camera.scale(-1)),
                    new WorldLineRenderer.Color(90, 170, 255, 145));
        }
    }

    static List<BlockPos> scan(final Minecraft client, final BlockPos center, final int radius, final int maxBlocks) {
        List<BlockPos> result = new ArrayList<>();
        for (int y = center.getY() - 4; y <= center.getY() + 4 && result.size() < maxBlocks; y++) {
            for (int x = center.getX() - radius; x <= center.getX() + radius && result.size() < maxBlocks; x++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius && result.size() < maxBlocks; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (client.level.isLoaded(pos) && isTunnel(client, pos)) {
                        result.add(pos.immutable());
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    static boolean isTunnel(final Minecraft client, final BlockPos pos) {
        if (!client.level.getBlockState(pos).isAir() || !client.level.getBlockState(pos.above()).isAir()
                || client.level.getBlockState(pos.below()).isAir()) {
            return false;
        }
        boolean northSouth = solid(client, pos.north()) && solid(client, pos.south())
                && client.level.getBlockState(pos.east()).isAir() && client.level.getBlockState(pos.west()).isAir();
        boolean eastWest = solid(client, pos.east()) && solid(client, pos.west())
                && client.level.getBlockState(pos.north()).isAir() && client.level.getBlockState(pos.south()).isAir();
        return northSouth || eastWest;
    }

    private static boolean solid(final Minecraft client, final BlockPos pos) {
        return client.level.getBlockState(pos).isFaceSturdy(client.level, pos, Direction.UP);
    }
}
