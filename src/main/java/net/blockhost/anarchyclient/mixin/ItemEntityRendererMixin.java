package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.blockhost.anarchyclient.module.impl.ItemPhysicsModule;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Unique
    private boolean anarchyclient$itemPhysicsPushed;

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void anarchyclient$applyItemPhysics(final ItemEntityRenderState state, final PoseStack poseStack,
                                                final SubmitNodeCollector submitNodeCollector,
                                                final CameraRenderState cameraRenderState, final CallbackInfo info) {
        ItemPhysicsModule.PhysicsTransform transform = ItemPhysicsModule.activeTransform();
        if (transform.identityTransform()) {
            return;
        }
        poseStack.pushPose();
        float scale = (float) transform.scale();
        poseStack.scale(scale, scale, scale);
        switch (transform.mode()) {
            case "Flat" -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case "Spin" -> poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * (float) transform.spinSpeed()));
            case "Float" -> {
                poseStack.translate(0.0, Math.sin(state.ageInTicks * 0.08) * 0.08, 0.0);
                poseStack.mulPose(Axis.YP.rotationDegrees(state.ageInTicks * (float) transform.spinSpeed()));
            }
            default -> {
            }
        }
        this.anarchyclient$itemPhysicsPushed = true;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN"))
    private void anarchyclient$restoreItemPhysics(final ItemEntityRenderState state, final PoseStack poseStack,
                                                  final SubmitNodeCollector submitNodeCollector,
                                                  final CameraRenderState cameraRenderState, final CallbackInfo info) {
        if (this.anarchyclient$itemPhysicsPushed) {
            poseStack.popPose();
            this.anarchyclient$itemPhysicsPushed = false;
        }
    }
}
