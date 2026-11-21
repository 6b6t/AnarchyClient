package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    /** Freecam moves the camera away from the player, so the player's in-wall, water, and fire overlays do not apply. */
    @Inject(method = "extractPlayerState", at = @At("TAIL"))
    private void anarchyclient$clearFreecamOverlays(
            final Camera camera,
            final DeltaTracker deltaTracker,
            final float worldPartialTicks,
            final PlayerRenderState state,
            final CallbackInfo info) {
        if (FreecamModule.detachesCamera()) {
            state.blockOverlay = null;
            state.waterOverlay = null;
            state.isEyeInWater = false;
            state.isOnFire = false;
        }
    }
}
