package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.ItemStopUseEvent;
import net.blockhost.anarchyclient.module.impl.AutoSprintModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void anarchyclient$stopUsingItem(final CallbackInfo info) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        AnarchyClient.MODULES.call(new ItemStopUseEvent(
                Minecraft.getInstance(),
                player.getUsedItemHand(),
                player.getUseItem().copy(),
                player.getUseItemRemainingTicks()
        ));
    }

    // Auto Sprint's omni sprint: vanilla only keeps sprinting with forward input, so count any movement input.
    @Redirect(
            method = {"shouldStopRunSprinting", "canStartSprinting"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;hasForwardImpulse()Z")
    )
    private boolean anarchyclient$omniSprintImpulse(final ClientInput input) {
        return input.hasForwardImpulse() || AutoSprintModule.omniSprintImpulse((LocalPlayer) (Object) this);
    }
}
