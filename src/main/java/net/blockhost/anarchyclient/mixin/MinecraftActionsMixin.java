package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.MultiActionsModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftActionsMixin {

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
