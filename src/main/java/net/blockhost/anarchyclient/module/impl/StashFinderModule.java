package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.HashSet;
import java.util.Set;

public final class StashFinderModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(16.0)
            .max(192.0)
            .step(16.0)
            .build()));
    private final NumberSetting threshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("threshold")
            .name("Threshold")
            .defaultValue(8.0)
            .min(2.0)
            .max(64.0)
            .step(1.0)
            .build()));

    private final Set<Long> reportedChunks = new HashSet<>();
    private int cooldownTicks;

    public StashFinderModule() {
        super("stash_finder", "Stash Finder", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.reportedChunks.clear();
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        this.cooldownTicks = 60;
        int chunkRadius = Math.max(1, (int) Math.ceil(this.range.value() / 16.0));
        ChunkPos center = player.chunkPosition();
        for (int chunkX = center.x() - chunkRadius; chunkX <= center.x() + chunkRadius; chunkX++) {
            for (int chunkZ = center.z() - chunkRadius; chunkZ <= center.z() + chunkRadius; chunkZ++) {
                if (!client.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                long key = ChunkPos.pack(chunkX, chunkZ);
                if (this.reportedChunks.contains(key)) {
                    continue;
                }
                LevelChunk chunk = client.level.getChunk(chunkX, chunkZ);
                int count = storageCount(chunk);
                if (count >= this.threshold.value().intValue()) {
                    this.reportedChunks.add(key);
                    BlockPos pos = chunk.getPos().getWorldPosition();
                    player.sendSystemMessage(Component.literal("Possible stash: " + count + " containers near "
                            + pos.getX() + ", " + pos.getZ()));
                }
            }
        }
    }

    static int storageCount(final LevelChunk chunk) {
        int count = 0;
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof ChestBlockEntity
                    || blockEntity instanceof ShulkerBoxBlockEntity
                    || blockEntity instanceof BaseContainerBlockEntity) {
                count++;
            }
        }
        return count;
    }
}
