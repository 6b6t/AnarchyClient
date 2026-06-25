package net.blockhost.anarchyclient.waypoint;

import net.minecraft.core.BlockPos;

public record Waypoint(String world, String name, BlockPos pos, int color) {

    public Waypoint {
        world = world == null || world.isBlank() ? "unknown" : world.trim();
        name = name == null || name.isBlank() ? "waypoint" : name.trim();
    }
}
