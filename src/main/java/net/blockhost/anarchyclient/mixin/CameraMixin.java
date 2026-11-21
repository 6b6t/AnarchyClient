package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.CameraTransformEvent;
import net.blockhost.anarchyclient.module.impl.FreeLookModule;
import net.blockhost.anarchyclient.module.impl.FreecamModule;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    // Mixin writes these fields of Camera itself; Camera reads them.
    @SuppressWarnings("UnusedVariable")
    @Shadow
    private float fovModifier;

    @SuppressWarnings("UnusedVariable")
    @Shadow
    private float oldFovModifier;

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract float yRot();

    @Shadow
    public abstract float xRot();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 position);

    /** Removes only vanilla's sprint, speed, and item-use FOV changes; module FOV hooks still apply. */
    @Inject(method = "tickFov", at = @At("TAIL"))
    private void anarchyclient$suppressDynamicFov(final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.DYNAMIC_FOV)) {
            this.oldFovModifier = 1.0F;
            this.fovModifier = 1.0F;
        }
    }

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$modifyFov(final float partialTicks, final CallbackInfoReturnable<Float> info) {
        info.setReturnValue(AnarchyClient.MODULES.fov(Minecraft.getInstance(), info.getReturnValue()));
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void anarchyclient$suppressBlockingEffects(
            final CameraRenderState cameraState, final DeltaTracker deltaTracker, final CallbackInfo info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.BLINDNESS)
                || RenderSuppression.suppresses(RenderSuppression.Kind.DARKNESS)) {
            cameraState.entityRenderState.doesMobEffectBlockSky = false;
        }
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$disableCameraClip(final float cameraDist, final CallbackInfoReturnable<Float> info) {
        if (RenderSuppression.suppresses(RenderSuppression.Kind.CAMERA_CLIP)) {
            info.setReturnValue(cameraDist);
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$freecamDetached(final CallbackInfoReturnable<Boolean> info) {
        if (FreecamModule.detachesCamera()) {
            info.setReturnValue(true);
        }
    }

    /** Free Look replaces the facing vanilla aligns the camera to, so third person still orbits the player. */
    @Redirect(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float anarchyclient$freeLookYaw(final Entity entity, final float partialTicks) {
        return FreeLookModule.viewYaw(entity, entity.getViewYRot(partialTicks));
    }

    @Redirect(
            method = "alignWithEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float anarchyclient$freeLookPitch(final Entity entity, final float partialTicks) {
        return FreeLookModule.viewPitch(entity, entity.getViewXRot(partialTicks));
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"))
    private void anarchyclient$modifyCamera(final DeltaTracker deltaTracker, final CallbackInfo info) {
        CameraTransformEvent event = AnarchyClient.MODULES.cameraTransform(
                Minecraft.getInstance(), this.position(), this.yRot(), this.xRot());
        this.setPosition(event.position());
        this.setRotation(event.yaw(), event.pitch());
    }
}
