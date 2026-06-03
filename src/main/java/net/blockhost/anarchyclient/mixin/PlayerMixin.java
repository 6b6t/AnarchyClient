package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
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
        if (AnarchyClient.MODULES.preventEdgeFall(Minecraft.getInstance(), (Player) (Object) this)) {
            info.setReturnValue(true);
        }
    }
}
