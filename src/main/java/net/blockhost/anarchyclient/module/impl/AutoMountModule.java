package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.EntityHitResult;

public final class AutoMountModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(20.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoMountModule() {
        super("auto_mount", "Auto Mount", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null
                || player.isPassenger()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        Entity target = nearestMount(client, player, this.range.value());
        if (target == null) {
            return;
        }
        InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        this.cooldownTicks = this.delay.value().intValue();
    }

    static Entity nearestMount(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!isMountCandidate(entity) || entity.getControllingPassenger() != null || !entity.isAlive()) {
                continue;
            }
            double distance = entity.distanceToSqr(player);
            if (distance <= rangeSqr && distance < bestDistance) {
                best = entity;
                bestDistance = distance;
            }
        }
        return best;
    }

    static boolean isMountCandidate(final Entity entity) {
        return entity instanceof AbstractHorse
                || entity instanceof Camel
                || entity instanceof AbstractBoat
                || entity instanceof Minecart
                || entity instanceof Pig
                || entity instanceof Strider;
    }
}
