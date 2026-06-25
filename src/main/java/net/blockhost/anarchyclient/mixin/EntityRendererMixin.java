package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.EspOutlineRegistry;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void anarchyclient$setOutlineColor(final T entity, final S state, final float partialTick,
                                               final CallbackInfo info) {
        int outlineColor = EspOutlineRegistry.get(entity.getId());
        if (outlineColor != EntityRenderState.NO_OUTLINE) {
            state.outlineColor = outlineColor;
        }
    }
}
