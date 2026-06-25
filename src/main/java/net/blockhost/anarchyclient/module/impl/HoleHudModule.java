package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class HoleHudModule extends HudElementModule {

    public HoleHudModule() {
        super("hole_hud", "Hole HUD", "Top Right");
    }

    @Override
    protected int color() {
        return this.inHole(Minecraft.getInstance()) ? 0xFF8EEAD5 : 0xFFE56A6A;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return List.of(this.inHole(client) ? "Hole safe" : "Hole unsafe");
    }

    private boolean inHole(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return false;
        }
        BlockPos pos = client.player.blockPosition();
        return HoleEspModule.isHole(
                client.level.getBlockState(pos).isAir(),
                client.level.getBlockState(pos.above()).isAir(),
                isSafe(client, pos.below()),
                isSafe(client, pos.north()),
                isSafe(client, pos.south()),
                isSafe(client, pos.east()),
                isSafe(client, pos.west())
        );
    }

    private static boolean isSafe(final Minecraft client, final BlockPos pos) {
        return !client.level.getBlockState(pos).getCollisionShape(client.level, pos).isEmpty();
    }
}
