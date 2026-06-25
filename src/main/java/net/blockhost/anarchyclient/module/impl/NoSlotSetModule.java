package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;

public final class NoSlotSetModule extends Module {

    public NoSlotSetModule() {
        super("no_slot_set", "No Slot Set", ModuleCategory.PLAYER);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.player != null && shouldCancel(packet);
    }

    static boolean shouldCancel(final Packet<?> packet) {
        return packet instanceof ClientboundSetHeldSlotPacket;
    }
}
