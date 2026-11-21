package net.blockhost.anarchyclient.module.impl;

import java.util.concurrent.atomic.AtomicReference;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;

public final class PortalGodModeModule extends Module {

    /**
     * Only the latest confirmation: the server re-sends its teleport about once a second while waiting and only
     * accepts the id of its current one, so keeping every confirmation grew without limit.
     */
    private final AtomicReference<ServerboundAcceptTeleportationPacket> held = new AtomicReference<>();

    public PortalGodModeModule() {
        super(
                "portal_god_mode",
                "Portal God Mode",
                ModuleCategory.MISC,
                "Holds back teleport confirmations, so after a dimension change the server keeps you in the "
                        + "invulnerable loading state. Turn it off to confirm the teleports and play normally.");
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ServerboundAcceptTeleportationPacket confirm) {
            this.held.set(confirm);
            return true;
        }
        return false;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.held.set(null);
    }

    @Override
    protected void onDisable() {
        ServerboundAcceptTeleportationPacket packet = this.held.getAndSet(null);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (packet != null && connection != null) {
            connection.send(packet);
        }
    }

    int heldCount() {
        return this.held.get() == null ? 0 : 1;
    }
}
