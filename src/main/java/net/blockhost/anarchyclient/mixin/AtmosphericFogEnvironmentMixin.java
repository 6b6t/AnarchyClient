package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.SkyboxModule;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AtmosphericFogEnvironment.class)
public abstract class AtmosphericFogEnvironmentMixin {

    @Inject(method = "getBaseColor", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$customFog(final ClientLevel level, final Camera camera, final int renderDistance,
                                         final float partialTicks, final CallbackInfoReturnable<Vector3fc> info) {
        Vector3fc color = SkyboxModule.fogColor();
        if (color != null && level.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD) {
            info.setReturnValue(color);
        }
    }
}
