package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;

public final class AntiReducedDebugInfoModule extends Module {

    public AntiReducedDebugInfoModule() {
        super("anti_reduced_debug_info", "Anti Reduced Debug", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null && client.player.isReducedDebugInfo()) {
            client.player.setReducedDebugInfo(false);
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.level != null
                && client.player != null
                && packet instanceof ClientboundEntityEventPacket event
                && event.getEventId() == 22
                && event.getEntity(client.level) == client.player;
    }
}
