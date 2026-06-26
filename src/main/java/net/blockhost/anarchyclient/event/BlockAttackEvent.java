package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class BlockAttackEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final BlockPos pos;
    private final Direction direction;

    public BlockAttackEvent(final Minecraft client, final BlockPos pos, final Direction direction) {
        this.client = client;
        this.pos = pos;
        this.direction = direction;
    }

    public Minecraft client() {
        return this.client;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public Direction direction() {
        return this.direction;
    }
}
