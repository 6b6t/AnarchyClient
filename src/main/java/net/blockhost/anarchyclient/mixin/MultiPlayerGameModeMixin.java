package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.AttackEntityEvent;
import net.blockhost.anarchyclient.event.BlockInteractEvent;
import net.blockhost.anarchyclient.event.EntityInteractEvent;
import net.blockhost.anarchyclient.event.ItemUseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$attackEntity(final Player player, final Entity target, final CallbackInfo info) {
        AttackEntityEvent event = AnarchyClient.MODULES.call(new AttackEntityEvent(Minecraft.getInstance(), player, target));
        if (event.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$useItemOn(final LocalPlayer player, final InteractionHand hand,
                                         final BlockHitResult blockHit,
                                         final CallbackInfoReturnable<InteractionResult> info) {
        BlockInteractEvent event = AnarchyClient.MODULES.call(new BlockInteractEvent(Minecraft.getInstance(), hand, blockHit));
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$entityInteract(final Player player, final Entity entity,
                                              final EntityHitResult hitResult, final InteractionHand hand,
                                              final CallbackInfoReturnable<InteractionResult> info) {
        EntityInteractEvent event = AnarchyClient.MODULES.call(
                new EntityInteractEvent(Minecraft.getInstance(), player, entity, hitResult, hand)
        );
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$useItem(final Player player, final InteractionHand hand,
                                       final CallbackInfoReturnable<InteractionResult> info) {
        ItemUseEvent event = AnarchyClient.MODULES.call(new ItemUseEvent(Minecraft.getInstance(), hand));
        if (event.isCancelled()) {
            info.setReturnValue(InteractionResult.FAIL);
        }
    }
}
