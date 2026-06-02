package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BlockScan {

    private BlockScan() {
    }

    static Set<Block> parseBlocks(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<Block> blocks = new LinkedHashSet<>();
        for (String token : value.split("[,|\\s]+")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            Identifier identifier = id.contains(":") ? Identifier.tryParse(id) : Identifier.withDefaultNamespace(id);
            if (identifier != null) {
                BuiltInRegistries.BLOCK.getOptional(identifier).ifPresent(blocks::add);
            }
        }
        return Set.copyOf(blocks);
    }

    static List<BlockPos> matchingBlocks(final Minecraft client, final Set<Block> blocks, final int horizontalRadius,
                                         final int verticalRadius, final int maxResults) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || blocks.isEmpty() || maxResults <= 0) {
            return List.of();
        }

        BlockPos center = player.blockPosition();
        int minY = Math.max(client.level.getMinY(), center.getY() - verticalRadius);
        int maxY = Math.min(client.level.getMinY() + client.level.getHeight() - 1, center.getY() + verticalRadius);
        List<BlockPos> results = new java.util.ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (int y = minY; y <= maxY && results.size() < maxResults; y++) {
            for (int x = center.getX() - horizontalRadius; x <= center.getX() + horizontalRadius && results.size() < maxResults; x++) {
                for (int z = center.getZ() - horizontalRadius; z <= center.getZ() + horizontalRadius && results.size() < maxResults; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!client.level.isLoaded(pos) || !blocks.contains(client.level.getBlockState(pos).getBlock())) {
                        continue;
                    }
                    long key = pos.asLong();
                    if (seen.add(key)) {
                        results.add(pos.immutable());
                    }
                }
            }
        }
        return List.copyOf(results);
    }
}
