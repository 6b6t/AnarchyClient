package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla sends the handshake from {@code initiateServerboundConnection} through the private {@code sendPacket},
 * which skips the public {@code send} hook in {@link ConnectionMixin}. This lets modules replace the handshake.
 * Cancelling it is not supported, because the connection cannot continue without it.
 */
@Mixin(Connection.class)
public abstract class ConnectionHandshakeMixin {

    @ModifyVariable(method = "sendPacket", at = @At("HEAD"), argsOnly = true)
    private Packet<?> anarchyclient$replaceHandshake(final Packet<?> packet) {
        if (!(packet instanceof ClientIntentionPacket) || PacketEventSilencer.silent()) {
            return packet;
        }
        PacketSendEvent event = AnarchyClient.MODULES.call(
                new PacketSendEvent(Minecraft.getInstance(), (Connection) (Object) this, packet));
        return event.packet();
    }
}
