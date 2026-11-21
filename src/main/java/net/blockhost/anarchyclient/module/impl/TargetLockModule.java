package net.blockhost.anarchyclient.module.impl;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TargetLockModule extends Module {

    private static TargetLockModule active;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(4.0)
            .max(128.0)
            .step(2.0)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));
    private UUID locked;

    public TargetLockModule() {
        super(
                "target_lock",
                "Target Lock",
                ModuleCategory.COMBAT,
                "Keeps combat modules on the same target by locking onto the nearest player until it leaves range.");
    }

    @Override
    protected void onEnable() {
        active = this;
        this.locked = null;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
        this.locked = null;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            this.locked = null;
            return;
        }
        LivingEntity current = this.findLocked(
                        client,
                        TargetPolicy.of(true, this.hostiles.value(), false, true, true, true, true),
                        this.range.value())
                .orElse(null);
        if (current != null) {
            return;
        }
        // Only the module's own range decides when the lock ends; callers may use a smaller range.
        this.locked = null;
        TargetQuery.closest(
                        client.level.entitiesForRendering(),
                        client.player,
                        TargetPolicy.of(true, this.hostiles.value(), false, true, true, true, true),
                        this.range.value(),
                        Comparator.comparingDouble(client.player::distanceToSqr))
                .ifPresent(target -> this.locked = target.getUUID());
    }

    public static Optional<LivingEntity> preferred(
            final Iterable<? extends Entity> entities,
            final Player player,
            final TargetPolicy policy,
            final double range) {
        TargetLockModule module = active;
        if (module == null || player == null) {
            return Optional.empty();
        }
        return module.findLocked(entities, player, policy, range);
    }

    private Optional<LivingEntity> findLocked(final Minecraft client, final TargetPolicy policy, final double range) {
        return client.level == null || client.player == null
                ? Optional.empty()
                : this.findLocked(client.level.entitiesForRendering(), client.player, policy, range);
    }

    private Optional<LivingEntity> findLocked(
            final Iterable<? extends Entity> entities,
            final Player player,
            final TargetPolicy policy,
            final double range) {
        if (this.locked == null) {
            return Optional.empty();
        }
        double rangeSqr = range * range;
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living
                    && entity.getUUID().equals(this.locked)
                    && player.distanceToSqr(entity) <= rangeSqr
                    && TargetQuery.allowed(entity, player, policy)) {
                return Optional.of(living);
            }
        }
        return Optional.empty();
    }
}
