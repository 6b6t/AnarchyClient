package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;

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
    /** Love mode (600 ticks) plus the parents' breeding cooldown (6000 ticks). */
    static final long REFEED_AFTER_TICKS = 6600L;

    private final Map<UUID, Long> fedAt = new HashMap<>();
    private int cooldownTicks;

    public AutoBreedModule() {
        super("auto_breed", "Auto Breed", ModuleCategory.WORLD,
                "Feeds the nearest adult animal with matching food from your hotbar, once per breeding cycle.");
    }

    @Override
    protected void onDisable() {
        this.fedAt.clear();
        this.cooldownTicks = 0;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.fedAt.clear();
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
        long now = client.level.getGameTime();
        this.fedAt.values().removeIf(time -> !recentlyFed(time, now));
        // The client never learns whether an animal is in love or on breeding cooldown, so remember
        // which animals this module fed and leave them alone for a full breeding cycle.
        Animal target = nearestBreedable(client, player, this.range.value(), animal -> !this.fedAt.containsKey(animal.getUUID()));
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
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            this.fedAt.put(target.getUUID(), now);
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    static boolean recentlyFed(final long fedTime, final long now) {
        return now - fedTime < REFEED_AFTER_TICKS && now >= fedTime;
    }

    static Animal nearestBreedable(final Minecraft client, final LocalPlayer player, final double range,
                                   final Predicate<Animal> allowed) {
        double rangeSqr = range * range;
        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            // Food given to a baby only speeds up its growth.
            if (!(entity instanceof Animal animal) || animal.isBaby() || !animal.isAlive() || !allowed.test(animal)
                    || !CombatTargets.canReachEntity(player, animal)) {
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
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && animal.isFood(stack)) {
                return true;
            }
        }
        return false;
    }
}
