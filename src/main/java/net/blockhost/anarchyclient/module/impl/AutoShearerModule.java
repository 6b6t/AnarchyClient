package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.OptionalInt;

public final class AutoShearerModule extends Module {

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
            .defaultValue(10.0)
            .min(1.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoShearerModule() {
        super("auto_shearer", "Auto Shearer", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        OptionalInt shears = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.SHEARS));
        if (shears.isEmpty()) {
            return;
        }
        Entity target = nearestShearable(client, player, this.range.value());
        if (target == null) {
            return;
        }
        InventoryActions.selectHotbarSlot(player, shears.orElseThrow());
        InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    static Entity nearestShearable(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Shearable shearable) || !shearable.readyForShearing()) {
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
}
