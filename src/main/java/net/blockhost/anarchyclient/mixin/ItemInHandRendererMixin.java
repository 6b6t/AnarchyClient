package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockhost.anarchyclient.module.impl.AnimationsModule;
import net.blockhost.anarchyclient.module.impl.HandViewModule;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private boolean anarchyclient$transformPushed;

    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$applyHandView(final float partialTick, final PoseStack poseStack,
                                             final SubmitNodeCollector submitNodeCollector,
                                             final LocalPlayer player, final int light, final CallbackInfo info) {
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
            poseStack.mulPose(Axis.XP.rotationDegrees((float) animation.pitch()));
            poseStack.mulPose(Axis.YP.rotationDegrees((float) animation.yaw()));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) animation.roll()));
            float scale = (float) transform.scale();
            poseStack.scale(scale, scale, scale);
            this.anarchyclient$transformPushed = true;
        }
    }

    @Inject(method = "submitHandsWithItems", at = @At("RETURN"))
    private void anarchyclient$restoreHandView(final float partialTick, final PoseStack poseStack,
                                               final SubmitNodeCollector submitNodeCollector,
                                               final LocalPlayer player, final int light, final CallbackInfo info) {
        if (this.anarchyclient$transformPushed) {
            poseStack.popPose();
            this.anarchyclient$transformPushed = false;
        }
    }
}
