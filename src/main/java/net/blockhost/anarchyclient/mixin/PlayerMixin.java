package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.module.impl.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$preventEdgeFall(final CallbackInfoReturnable<Boolean> info) {
        PreventEdgeFallEvent event = AnarchyClient.MODULES.call(new PreventEdgeFallEvent(Minecraft.getInstance(), (Player) (Object) this));
        if (event.isCancelled()) {
            info.setReturnValue(true);
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$extendBlockReach(final CallbackInfoReturnable<Double> info) {
        if (this.anarchyclient$isLocalPlayer()) {
            info.setReturnValue(info.getReturnValue() + ReachModule.blockBonus());
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$extendEntityReach(final CallbackInfoReturnable<Double> info) {
        if (this.anarchyclient$isLocalPlayer()) {
            info.setReturnValue(info.getReturnValue() + ReachModule.entityBonus());
        }
    }

    /**
     * Returns whether this is the client's own player, or its server-side copy on the singleplayer or
     * LAN server this client hosts. That server checks reach with its own player, so the bonus must
     * apply there too. Other players on a LAN world keep vanilla reach.
     */
    @Unique
    private boolean anarchyclient$isLocalPlayer() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer localPlayer = client.player;
        if (localPlayer == null) {
            return false;
        }
        if ((Object) this == localPlayer) {
            return true;
        }
        IntegratedServer server = client.getSingleplayerServer();
        return server != null
                && (Object) this instanceof ServerPlayer serverPlayer
                && serverPlayer.level().getServer() == server
                && serverPlayer.getUUID().equals(localPlayer.getUUID());
    }
}
