package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class MapHudModule extends HudElementModule {

    public MapHudModule() {
        super("map_hud", "Map HUD", "Top Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        BlockPos pos = client.player.blockPosition();
        return List.of(
                "Chunk " + Math.floorDiv(pos.getX(), 16) + ", " + Math.floorDiv(pos.getZ(), 16),
                "Region " + Math.floorDiv(pos.getX(), 512) + ", " + Math.floorDiv(pos.getZ(), 512),
                "Biome " + client.level.getBiome(pos).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown")
        );
    }
}
