package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.OptionalInt;

public final class AutoBreedModule extends Module {

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
            .defaultValue(8.0)
            .min(1.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoBreedModule() {
        super("auto_breed", "Auto Breed", ModuleCategory.WORLD);
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
        Animal target = nearestBreedable(client, player, this.range.value());
        if (target == null) {
            return;
        }
        OptionalInt food = InventoryActions.findHotbarSlot(player.getInventory(), target::isFood);
        if (food.isEmpty()) {
            return;
        }
        InventoryActions.selectHotbarSlot(player, food.orElseThrow());
        InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    static Animal nearestBreedable(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Animal animal) || !animal.canFallInLove()) {
                continue;
            }
            double distance = animal.distanceToSqr(player);
            if (distance > rangeSqr || distance >= bestDistance) {
                continue;
            }
            if (!hasFood(player, animal)) {
                continue;
            }
            best = animal;
            bestDistance = distance;
        }
        return best;
    }

    private static boolean hasFood(final LocalPlayer player, final Animal animal) {
        for (int slot = 0; slot < net.minecraft.world.entity.player.Inventory.getSelectionSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && animal.isFood(stack)) {
                return true;
            }
        }
        return false;
    }
}
