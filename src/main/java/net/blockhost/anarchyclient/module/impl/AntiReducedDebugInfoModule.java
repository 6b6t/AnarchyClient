package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.EntityEvent;

public final class AntiReducedDebugInfoModule extends Module {

    public AntiReducedDebugInfoModule() {
        super("anti_reduced_debug_info", "Anti Reduced Debug", ModuleCategory.MISC,
                "Keeps full coordinates and other F3 debug details visible on servers that enable reduced debug info.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null && client.player.isReducedDebugInfo()) {
            client.player.setReducedDebugInfo(false);
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        // Runs on the network thread, so no entity lookup: the flag only matters for the local player, and the tick
        // clears it anyway if one slips through.
        return packet instanceof ClientboundEntityEventPacket event
                && event.getEventId() == EntityEvent.REDUCED_DEBUG_INFO;
    }
}
