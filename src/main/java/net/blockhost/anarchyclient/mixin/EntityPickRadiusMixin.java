package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.HitboxModule;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityPickRadiusMixin {

    @Inject(method = "getPickRadius", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$expandHitbox(final CallbackInfoReturnable<Float> info) {
        float bonus = HitboxModule.pickRadiusBonus((Entity) (Object) this);
        if (bonus > 0.0F) {
            info.setReturnValue(info.getReturnValue() + bonus);
        }
    }
}
