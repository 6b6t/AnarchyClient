package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.NoSwingModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySwingMixin {

    @Inject(
            method =
                    "swing(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/component/SwingAnimation;Z)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void anarchyclient$noSwing(
            final InteractionHand hand,
            final SwingAnimation animation,
            final boolean sendToSwingingEntity,
            final CallbackInfoReturnable<Boolean> info) {
        if (NoSwingModule.shouldHideSwing(Minecraft.getInstance(), (LivingEntity) (Object) this)) {
            info.setReturnValue(false);
        }
    }
}
