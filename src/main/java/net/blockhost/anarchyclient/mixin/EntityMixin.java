package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.NoPushModule;
import net.blockhost.anarchyclient.module.impl.NoSlowModule;
import net.blockhost.anarchyclient.module.impl.NoWebModule;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noWeb(final BlockState state, final Vec3 speedMultiplier, final CallbackInfo info) {
        if ((Object) this != Minecraft.getInstance().player) {
            return;
        }
        if (NoWebModule.shouldIgnore(state) || NoSlowModule.shouldIgnoreStuckBlock(state)) {
            info.cancel();
        }
    }

    @Redirect(
            method = "getBlockSpeedFactor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getSpeedFactor()F"))
    private float anarchyclient$noSlowSpeedFactor(final Block block) {
        return NoSlowModule.blockSpeedFactor((Entity) (Object) this, block);
    }

    @Redirect(
            method = "applyEffectsFromBlocks(Ljava/util/List;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private void anarchyclient$noSlowStepOn(
            final Block block, final Level level, final BlockPos pos, final BlockState state, final Entity entity) {
        if (!NoSlowModule.shouldIgnoreStepOn(entity, state)) {
            block.stepOn(level, pos, state, entity);
        }
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$noPush(final double x, final double y, final double z, final CallbackInfo info) {
        if (NoPushModule.shouldCancelPush(Minecraft.getInstance(), (Entity) (Object) this)) {
            info.cancel();
        }
    }

    @Redirect(
            method = "updateFluidInteraction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z"))
    private boolean anarchyclient$noFluidPush(final Entity entity) {
        return !NoPushModule.shouldCancelFluidPush(Minecraft.getInstance(), entity) && entity.isPushedByFluid();
    }
}
