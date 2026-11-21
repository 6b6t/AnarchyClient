package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

public final class NoSlotSetModule extends Module {

    public NoSlotSetModule() {
        super(
                "no_slot_set",
                "No Slot Set",
                ModuleCategory.PLAYER,
                "Ignores server packets that change the selected hotbar slot and keeps the server on your slot.");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null || !shouldCancel(packet)) {
            return false;
        }
        // The server already switched its slot; tell it which slot the client still holds so both agree.
        MainThread.run(() -> {
            if (client.player != null) {
                connection.send(new ServerboundSetCarriedItemPacket(
                        client.player.getInventory().getSelectedSlot()));
            }
        });
        return true;
    }

    static boolean shouldCancel(final Packet<?> packet) {
        return packet instanceof ClientboundSetHeldSlotPacket;
    }
}
