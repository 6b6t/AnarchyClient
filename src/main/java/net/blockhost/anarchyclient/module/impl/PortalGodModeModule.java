package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;

import java.util.ArrayList;
import java.util.List;

public final class PortalGodModeModule extends Module {

    private final List<ServerboundAcceptTeleportationPacket> held = new ArrayList<>();

    public PortalGodModeModule() {
        super("portal_god_mode", "Portal God Mode", ModuleCategory.MISC,
                "Holds back teleport confirmations, so after a dimension change the server keeps you in the "
                        + "invulnerable loading state. Turn it off to confirm the teleports and play normally.");
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ServerboundAcceptTeleportationPacket confirm) {
            synchronized (this.held) {
                this.held.add(confirm);
            }
            return true;
        }
        return false;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        synchronized (this.held) {
            this.held.clear();
        }
    }

    @Override
    protected void onDisable() {
        List<ServerboundAcceptTeleportationPacket> packets;
        synchronized (this.held) {
            packets = List.copyOf(this.held);
            this.held.clear();
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        for (ServerboundAcceptTeleportationPacket packet : packets) {
            connection.send(packet);
        }
    }

    int heldCount() {
        synchronized (this.held) {
            return this.held.size();
        }
    }
}
