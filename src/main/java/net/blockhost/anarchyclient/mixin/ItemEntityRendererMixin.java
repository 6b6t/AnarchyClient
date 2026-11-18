package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockhost.anarchyclient.module.impl.ItemPhysicsModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Item Physics replaces vanilla's bob (the translate) and spin (the rotate) inside {@code submit}, so the
 * module's transforms are not applied on top of, or inside, vanilla's.
 */
@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    /** Height that keeps a flat item's model just above the ground. */
    @Unique
    private static final float FLAT_LIFT = 0.02F;

    @Unique
    private ItemEntityRenderState anarchyclient$state;

    @Unique
    private boolean anarchyclient$scalePushed;

    @Inject(method = SUBMIT, at = @At("HEAD"))
    private void anarchyclient$beginItemPhysics(final ItemEntityRenderState state, final PoseStack poseStack,
                                                final SubmitNodeCollector submitNodeCollector,
                                                final CameraRenderState cameraRenderState, final CallbackInfo info) {
        this.anarchyclient$state = state;
        double scale = ItemPhysicsModule.activeTransform().scale();
        if (scale != 1.0) {
            poseStack.pushPose();
            poseStack.scale((float) scale, (float) scale, (float) scale);
            this.anarchyclient$scalePushed = true;
        }
    }

    @Redirect(method = SUBMIT, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
    private void anarchyclient$replaceBob(final PoseStack poseStack, final float x, final float y, final float z) {
        ItemEntityRenderState state = this.anarchyclient$state;
        String mode = ItemPhysicsModule.activeTransform().mode();
        if (state == null || "Vanilla".equals(mode) || "Spin".equals(mode)) {
            poseStack.translate(x, y, z);
            return;
        }
        // Vanilla's y is the model's ground offset plus the bob; take the bob back out.
        float bob = Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
        float groundOffset = y - bob;
        if ("Flat".equals(mode)) {
            poseStack.translate(x, FLAT_LIFT, z);
        } else {
            poseStack.translate(x, groundOffset + 0.1F + Mth.sin(state.ageInTicks * 0.08F) * 0.08F, z);
        }
    }

    @Redirect(method = SUBMIT, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;rotate(Lcom/mojang/math/Axis;F)V", ordinal = 0))
    private void anarchyclient$replaceSpin(final PoseStack poseStack, final Axis axis, final float spin) {
        ItemEntityRenderState state = this.anarchyclient$state;
        ItemPhysicsModule.PhysicsTransform transform = ItemPhysicsModule.activeTransform();
        if (state == null || "Vanilla".equals(transform.mode())) {
            poseStack.rotate(axis, spin);
            return;
        }
        if ("Flat".equals(transform.mode())) {
            // A fixed facing per item (from its bob offset), then lay it on its side.
            poseStack.rotate(Axis.YP, state.bobOffset);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            return;
        }
        poseStack.rotateDegrees(Axis.YP, state.ageInTicks * (float) transform.spinSpeed());
    }

    @Inject(method = SUBMIT, at = @At("RETURN"))
    private void anarchyclient$endItemPhysics(final ItemEntityRenderState state, final PoseStack poseStack,
                                              final SubmitNodeCollector submitNodeCollector,
                                              final CameraRenderState cameraRenderState, final CallbackInfo info) {
        if (this.anarchyclient$scalePushed) {
            poseStack.popPose();
            this.anarchyclient$scalePushed = false;
        }
        this.anarchyclient$state = null;
    }
}
