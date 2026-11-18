package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.FullbrightModule;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public abstract class LightmapRenderStateExtractorMixin {

    /**
     * Fullbright raises the lightmap brightness directly. Writing it into the gamma option instead would put an
     * out-of-range value into options.txt, which vanilla then drops along with the player's own setting.
     */
    @Inject(method = "extract", at = @At("TAIL"))
    private void anarchyclient$fullbright(final LightmapRenderState renderState, final float partialTicks,
                                          final CallbackInfo info) {
        float brightness = FullbrightModule.brightness();
        if (renderState.needsUpdate && brightness >= 0.0F) {
            renderState.brightness = brightness;
        }
    }
}
