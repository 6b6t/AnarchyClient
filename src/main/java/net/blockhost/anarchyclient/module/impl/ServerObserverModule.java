package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public final class ServerObserverModule extends Module {

    public ServerObserverModule() {
        super("server_observer", "Server Observer", ModuleCategory.MISC);
        this.enabled(true);
    }

    @Override
    public void tick(final Minecraft client) {
        ServerObserver.observeTick(client);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        ServerObserver.observeReceived(client, packet);
        return false;
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        ServerObserver.observeJoined(client);
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        ServerObserver.observeLeft();
    }
}
