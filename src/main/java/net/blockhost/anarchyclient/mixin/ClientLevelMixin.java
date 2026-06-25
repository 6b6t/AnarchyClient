package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.BlockBreakingProgressEvent;
import net.blockhost.anarchyclient.event.EntityAddedEvent;
import net.blockhost.anarchyclient.event.EntityRemovedEvent;
import net.blockhost.anarchyclient.event.ParticleEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.entity.LevelEntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();

    @Inject(method = "addEntity", at = @At("RETURN"))
    private void anarchyclient$entityAdded(final Entity entity, final CallbackInfo info) {
        AnarchyClient.MODULES.call(new EntityAddedEvent(Minecraft.getInstance(), entity));
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void anarchyclient$entityRemoved(final int id, final RemovalReason reason, final CallbackInfo info) {
        Entity entity = this.getEntities().get(id);
        if (entity != null) {
            AnarchyClient.MODULES.call(new EntityRemovedEvent(Minecraft.getInstance(), entity, reason));
        }
    }

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"))
    private void anarchyclient$blockBreakingProgress(final int id, final BlockPos pos, final int progress,
                                                     final CallbackInfo info) {
        AnarchyClient.MODULES.call(new BlockBreakingProgressEvent(Minecraft.getInstance(), id, pos, progress));
    }

    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$particle(final ParticleOptions particle, final boolean overrideLimiter,
                                        final boolean alwaysShowParticles, final double x, final double y,
                                        final double z, final double xd, final double yd, final double zd,
                                        final CallbackInfo info) {
        ParticleEvent event = AnarchyClient.MODULES.call(new ParticleEvent(Minecraft.getInstance(), particle, alwaysShowParticles));
        if (event.isCancelled()) {
            info.cancel();
        }
    }
}
