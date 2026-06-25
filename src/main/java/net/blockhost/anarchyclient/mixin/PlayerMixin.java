package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.PreventEdgeFallEvent;
import net.blockhost.anarchyclient.module.impl.ReachModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
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
        info.setReturnValue(info.getReturnValue() + ReachModule.blockBonus());
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$extendEntityReach(final CallbackInfoReturnable<Double> info) {
        info.setReturnValue(info.getReturnValue() + ReachModule.entityBonus());
    }
}
