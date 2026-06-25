package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.GameJoinedEvent;
import net.blockhost.anarchyclient.event.GameLeftEvent;
import net.blockhost.anarchyclient.event.SoundPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void anarchyclient$handleSoundEvent(final ClientboundSoundPacket packet, final CallbackInfo info) {
        AnarchyClient.MODULES.call(new SoundPacketEvent(Minecraft.getInstance(), packet));
    }

    @Inject(method = "handleLogin", at = @At("RETURN"))
    private void anarchyclient$handleLogin(final ClientboundLoginPacket packet, final CallbackInfo info) {
        AnarchyClient.MODULES.call(new GameJoinedEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }

    @Inject(method = "clearLevel", at = @At("HEAD"))
    private void anarchyclient$clearLevel(final CallbackInfo info) {
        AnarchyClient.MODULES.call(new GameLeftEvent(Minecraft.getInstance(), (ClientPacketListener) (Object) this));
    }
}
