package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public final class IllegalDisconnectModule extends Module {

    public IllegalDisconnectModule() {
        super("illegal_disconnect", "Illegal Disconnect", ModuleCategory.MISC,
                "Sends an invalid movement packet so the server kicks you right away, then turns itself off. "
                        + "Use it to leave instantly on servers that keep you online after a normal disconnect.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (listener != null) {
            IllegalDisconnect.send(listener.getConnection());
        }
        this.enabled(false);
    }
}
