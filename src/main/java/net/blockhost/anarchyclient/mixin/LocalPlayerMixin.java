package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.ItemStopUseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Shadow
    public abstract InteractionHand getUsedItemHand();

    @Shadow
    public abstract ItemStack getUseItem();

    @Shadow
    public abstract int getUseItemRemainingTicks();

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void anarchyclient$stopUsingItem(final CallbackInfo info) {
        AnarchyClient.MODULES.call(new ItemStopUseEvent(
                Minecraft.getInstance(),
                this.getUsedItemHand(),
                this.getUseItem().copy(),
                this.getUseItemRemainingTicks()
        ));
    }
}
