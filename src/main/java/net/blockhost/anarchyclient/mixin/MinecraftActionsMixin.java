package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.inventory.ItemUseControl;
import net.blockhost.anarchyclient.module.impl.MultiActionsModule;
import net.blockhost.anarchyclient.module.impl.NoMiningTraceModule;
import net.blockhost.anarchyclient.world.MiningControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftActionsMixin {

    @Shadow
    public HitResult hitResult;

    @Shadow
    @Final
    public Options options;

    @Shadow
    public LocalPlayer player;

    @Shadow
    private boolean startAttack() {
        throw new AssertionError();
    }

    @Unique
    private boolean anarchyclient$holdItemUse;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void anarchyclient$checkItemUseHold(final CallbackInfo info) {
        this.anarchyclient$holdItemUse = ItemUseControl.suppressVanillaRelease();
    }

    /**
     * Multi Actions: vanilla throws attack clicks away while an item is in use, so run them first. Only the
     * isHandsBusy check in startAttack is bypassed further down.
     */
    @Inject(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private void anarchyclient$attackWhileUsing(final CallbackInfo info) {
        if (this.player != null && this.player.isUsingItem() && MultiActionsModule.attackWhileUsing()) {
            while (this.options.keyAttack.consumeClick()) {
                this.startAttack();
            }
        }
    }

    @Redirect(
            method = "handleKeybinds",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;releaseUsingItem(Lnet/minecraft/world/entity/player/Player;)V"))
    private void anarchyclient$keepHeldItemUse(final MultiPlayerGameMode gameMode, final Player player) {
        if (!this.anarchyclient$holdItemUse) {
            gameMode.releaseUsingItem(player);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void anarchyclient$replaceTraceForMining(final CallbackInfoReturnable<Boolean> info) {
        this.hitResult = NoMiningTraceModule.replacementHitResult((Minecraft) (Object) this, this.hitResult);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$continueAttack(final boolean down, final CallbackInfo info) {
        if (MiningControl.suppressVanillaAttack()) {
            info.cancel();
            return;
        }
        this.hitResult = NoMiningTraceModule.replacementHitResult((Minecraft) (Object) this, this.hitResult);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void anarchyclient$replaceTraceForUse(final CallbackInfo info) {
        this.hitResult = NoMiningTraceModule.replacementHitResult((Minecraft) (Object) this, this.hitResult);
    }

    @Redirect(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean anarchyclient$allowStartAttackWhileUsing(final LocalPlayer player) {
        return !MultiActionsModule.attackWhileUsing() && player.isHandsBusy();
    }

    @Redirect(
            method = "continueAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean anarchyclient$allowBreakWhileUsing(final LocalPlayer player) {
        return !MultiActionsModule.breakWhileUsing() && player.isUsingItem();
    }

    @Redirect(
            method = "startUseItem",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean anarchyclient$allowPlaceWhileBreaking(final MultiPlayerGameMode gameMode) {
        return !MultiActionsModule.placeWhileBreaking() && gameMode.isDestroying();
    }

    @Redirect(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z"))
    private boolean anarchyclient$allowUseWhileBusy(final LocalPlayer player) {
        return !MultiActionsModule.useWhileBusy() && player.isHandsBusy();
    }
}
