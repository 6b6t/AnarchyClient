package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.SkyboxModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void anarchyclient$customSky(
            final ClientLevel level,
            final float partialTicks,
            final Camera camera,
            final SkyRenderState state,
            final CallbackInfo info) {
        if (state.skybox != DimensionType.Skybox.OVERWORLD) {
            return;
        }
        Vector3fc color = SkyboxModule.skyColor();
        if (color != null) {
            state.skyColor = color;
        }
        if (SkyboxModule.forceStars()) {
            state.starBrightness = Math.max(state.starBrightness, 1.0F);
        }
    }
}
