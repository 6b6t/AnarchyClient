package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.NoPushModule;
import net.blockhost.anarchyclient.module.impl.NoWebModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noWeb(final BlockState state, final Vec3 speedMultiplier, final CallbackInfo info) {
        if (NoWebModule.shouldIgnore(state)) {
            info.cancel();
        }
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noPush(final double x, final double y, final double z, final CallbackInfo info) {
        if (NoPushModule.shouldCancelPush(Minecraft.getInstance(), (Entity) (Object) this)) {
            info.cancel();
        }
    }
}
