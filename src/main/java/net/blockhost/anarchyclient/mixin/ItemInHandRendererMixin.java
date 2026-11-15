package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockhost.anarchyclient.module.impl.AnimationsModule;
import net.blockhost.anarchyclient.module.impl.HandViewModule;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private boolean anarchyclient$transformPushed;

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$applyHandView(final float partialTick, final PoseStack poseStack,
                                             final SubmitNodeCollector submitNodeCollector,
                                             final PlayerRenderState playerState,
                                             final FirstPersonHandsAndItemsRenderState state, final CallbackInfo info) {
        HandViewModule.HandTransform transform = HandViewModule.activeTransform();
        if (transform.hidden()) {
            info.cancel();
            return;
        }
        AnimationsModule.AnimationTransform animation = AnimationsModule.activeTransform();
        if (!transform.identity() || !animation.identityTransform()) {
            poseStack.pushPose();
            poseStack.translate(transform.x(), transform.y(), 0.0);
            poseStack.translate(animation.x(), animation.y(), animation.z());
            poseStack.rotateDegrees(Axis.XP, (float) animation.pitch());
            poseStack.rotateDegrees(Axis.YP, (float) animation.yaw());
            poseStack.rotateDegrees(Axis.ZP, (float) animation.roll());
            float scale = (float) transform.scale();
            poseStack.scale(scale, scale, scale);
            this.anarchyclient$transformPushed = true;
        }
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void anarchyclient$restoreHandView(final float partialTick, final PoseStack poseStack,
                                               final SubmitNodeCollector submitNodeCollector,
                                               final PlayerRenderState playerState,
                                             final FirstPersonHandsAndItemsRenderState state, final CallbackInfo info) {
        if (this.anarchyclient$transformPushed) {
            poseStack.popPose();
            this.anarchyclient$transformPushed = false;
        }
    }
}
