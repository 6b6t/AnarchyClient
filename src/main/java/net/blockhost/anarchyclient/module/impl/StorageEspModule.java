package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class StorageEspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(16.0)
            .max(192.0)
            .step(8.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(180.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final BooleanSetting shulkers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("shulkers")
            .name("Shulkers")
            .defaultValue(true)
            .build()));
    private List<BlockPos> cachedPositions = List.of();
    private int scanCooldownTicks;

    public StorageEspModule() {
        super("storage_esp", "Storage ESP", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.scanCooldownTicks > 0) {
            this.scanCooldownTicks--;
            return;
        }
        this.cachedPositions = this.scan(client);
        this.scanCooldownTicks = 20;
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
        int alpha = this.opacity.value().intValue();
        for (BlockPos pos : this.cachedPositions) {
            WorldLineRenderer.box(matrices, submits, new AABB(pos).move(camera.scale(-1)), color(client.level.getBlockEntity(pos), alpha));
        }
    }

    private List<BlockPos> scan(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return List.of();
        }
        int chunkRadius = Math.max(1, (int) Math.ceil(this.range.value() / 16.0));
        double rangeSqr = this.range.value() * this.range.value();
        ChunkPos center = player.chunkPosition();
        List<BlockPos> positions = new java.util.ArrayList<>();
        for (int chunkX = center.x() - chunkRadius; chunkX <= center.x() + chunkRadius; chunkX++) {
            for (int chunkZ = center.z() - chunkRadius; chunkZ <= center.z() + chunkRadius; chunkZ++) {
                if (!client.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = client.level.getChunk(chunkX, chunkZ);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (isStorage(blockEntity) && Vec3.atCenterOf(blockEntity.getBlockPos()).distanceToSqr(player.position()) <= rangeSqr) {
                        positions.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        return List.copyOf(positions);
    }

    private boolean isStorage(final BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof EnderChestBlockEntity
                || blockEntity instanceof BaseContainerBlockEntity && !(blockEntity instanceof ShulkerBoxBlockEntity)
                || this.shulkers.value() && blockEntity instanceof ShulkerBoxBlockEntity;
    }

    private static WorldLineRenderer.Color color(final BlockEntity blockEntity, final int alpha) {
        if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return new WorldLineRenderer.Color(190, 116, 255, alpha);
        }
        if (blockEntity instanceof EnderChestBlockEntity) {
            return new WorldLineRenderer.Color(88, 255, 180, alpha);
        }
        return new WorldLineRenderer.Color(255, 196, 76, alpha);
    }
}
