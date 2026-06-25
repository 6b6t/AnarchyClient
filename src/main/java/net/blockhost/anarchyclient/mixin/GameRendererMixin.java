package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noHurtCamera(final CameraRenderState cameraState, final PoseStack poseStack,
                                            final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.HURT_CAMERA)) {
            info.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noViewBob(final CameraRenderState cameraState, final PoseStack poseStack,
                                         final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.VIEW_BOB)) {
            info.cancel();
        }
    }
}
