package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class NewChunksModule extends Module {

    private static final Direction[] SOURCE_SEARCH_DIRECTIONS = {
            Direction.EAST,
            Direction.NORTH,
            Direction.WEST,
            Direction.SOUTH,
            Direction.UP
    };

    private final BooleanSetting clearOnDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("clear_on_disable")
            .name("Clear")
            .defaultValue(true)
            .build()));
    private final BooleanSetting inspectChunkPackets = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("inspect_chunk_packets")
            .name("Inspect")
            .defaultValue(false)
            .build()));
    private final NumberSetting renderY = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("render_y")
            .name("Y")
            .defaultValue(64.0)
            .min(-64.0)
            .max(320.0)
            .step(4.0)
            .build()));
    private final NumberSetting renderRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("render_range")
            .name("Range")
            .defaultValue(1024.0)
            .min(128.0)
            .max(4096.0)
            .step(128.0)
            .build()));
    private final Set<Long> newChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> oldChunks = ConcurrentHashMap.newKeySet();

    public NewChunksModule() {
        super("new_chunks", "New Chunks", ModuleCategory.RENDER);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.level == null) {
            return false;
        }
        if (packet instanceof ClientboundSectionBlocksUpdatePacket update) {
            update.runUpdates((pos, state) -> this.markFluidUpdate(client, pos, state.getFluidState()));
        } else if (packet instanceof ClientboundBlockUpdatePacket update) {
            this.markFluidUpdate(client, update.getPos(), update.getBlockState().getFluidState());
        } else if (this.inspectChunkPackets.value() && packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            this.inspectChunkPacket(client, chunkPacket);
        }
        return false;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || client.player == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double rangeSqr = this.renderRange.value() * this.renderRange.value();
        this.renderChunks(matrices, submits, camera, client.player.blockPosition(), rangeSqr, this.oldChunks,
                new WorldLineRenderer.Color(90, 220, 120, 40), new WorldLineRenderer.Color(90, 220, 120, 170));
        this.renderChunks(matrices, submits, camera, client.player.blockPosition(), rangeSqr, this.newChunks,
                new WorldLineRenderer.Color(245, 80, 80, 55), new WorldLineRenderer.Color(245, 80, 80, 220));
    }

    @Override
    protected void onDisable() {
        if (this.clearOnDisable.value()) {
            this.newChunks.clear();
            this.oldChunks.clear();
        }
    }

    private void markFluidUpdate(final Minecraft client, final BlockPos pos, final FluidState fluid) {
        if (!isFlowingFluid(fluid)) {
            return;
        }
        long chunk = ChunkPos.pack(pos);
        if (this.oldChunks.contains(chunk)) {
            return;
        }
        for (Direction direction : SOURCE_SEARCH_DIRECTIONS) {
            BlockPos neighbor = pos.relative(direction);
            if (client.level != null && client.level.isLoaded(neighbor)
                    && client.level.getFluidState(neighbor).isSource()) {
                this.newChunks.add(chunk);
                return;
            }
        }
    }

    private void inspectChunkPacket(final Minecraft client, final ClientboundLevelChunkWithLightPacket packet) {
        if (client.level == null) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(packet.getX(), packet.getZ());
        long chunk = chunkPos.pack();
        if (this.newChunks.contains(chunk) || this.oldChunks.contains(chunk)) {
            return;
        }
        LevelChunk chunkView = new LevelChunk(client.level, chunkPos);
        try {
            chunkView.replaceWithPacketData(
                    packet.getChunkData().getReadBuffer(),
                    packet.getChunkData().getHeightmaps(),
                    packet.getChunkData().getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ())
            );
        } catch (RuntimeException exception) {
            return;
        }
        int minY = client.level.getMinY();
        int maxY = minY + client.level.getHeight();
        for (int y = minY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (isFlowingFluid(chunkView.getFluidState(x, y, z))) {
                        this.oldChunks.add(chunk);
                        return;
                    }
                }
            }
        }
    }

    private void renderChunks(final PoseStack matrices, final SubmitNodeCollector submits, final Vec3 camera,
                              final BlockPos playerPos, final double rangeSqr, final Set<Long> chunks,
                              final WorldLineRenderer.Color fillColor, final WorldLineRenderer.Color lineColor) {
        int y = this.renderY.value().intValue();
        for (long key : chunks) {
            ChunkPos chunk = ChunkPos.unpack(key);
            BlockPos center = chunk.getMiddleBlockPosition(y);
            if (center.distSqr(playerPos) > rangeSqr) {
                continue;
            }
            AABB box = new AABB(
                    chunk.getMinBlockX(),
                    y,
                    chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1,
                    y + 1,
                    chunk.getMaxBlockZ() + 1
            ).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, fillColor);
            WorldLineRenderer.boxNoDepth(matrices, submits, box, lineColor);
        }
    }

    static boolean isFlowingFluid(final FluidState fluid) {
        return fluid != null && !fluid.isEmpty() && !fluid.isSource();
    }
}
