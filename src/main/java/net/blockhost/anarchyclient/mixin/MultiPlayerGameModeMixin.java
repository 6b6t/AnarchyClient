package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.AttackEntityEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$attackEntity(final Player player, final Entity target, final CallbackInfo info) {
        AttackEntityEvent event = AnarchyClient.MODULES.call(new AttackEntityEvent(Minecraft.getInstance(), player, target));
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}
