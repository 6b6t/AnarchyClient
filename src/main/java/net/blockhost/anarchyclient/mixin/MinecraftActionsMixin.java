package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.MultiActionsModule;
import net.blockhost.anarchyclient.module.impl.NoMiningTraceModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftActionsMixin {

    @Shadow
    public HitResult hitResult;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void anarchyclient$replaceTraceForMining(final CallbackInfoReturnable<Boolean> info) {
        this.hitResult = NoMiningTraceModule.replacementHitResult((Minecraft) (Object) this, this.hitResult);
    }

    @Redirect(
            method = "continueAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z")
    )
    private boolean anarchyclient$allowAttackWhileUsing(final LocalPlayer player) {
        return !MultiActionsModule.attackWhileUsing() && player.isUsingItem();
    }

    @Redirect(
            method = "startUseItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")
    )
    private boolean anarchyclient$allowUseWhileBusy(final LocalPlayer player) {
        return !MultiActionsModule.useWhileBusy() && player.isHandsBusy();
    }
}
