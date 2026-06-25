package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.CameraTransformEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

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

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$modifyFov(final float partialTicks, final CallbackInfoReturnable<Float> info) {
        info.setReturnValue(AnarchyClient.MODULES.fov(Minecraft.getInstance(), info.getReturnValue()));
    }

    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F"))
    private void anarchyclient$modifyCamera(final DeltaTracker deltaTracker, final CallbackInfo info) {
        CameraTransformEvent event = AnarchyClient.MODULES.cameraTransform(
                Minecraft.getInstance(),
                this.position(),
                this.yRot(),
                this.xRot()
        );
        this.setPosition(event.position());
        this.setRotation(event.yaw(), event.pitch());
    }
}
