package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

public final class AutoNametagModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
            .build()));
    private final Set<UUID> tagged = new HashSet<>();

    public AutoNametagModule() {
        super("auto_nametag", "Auto Nametag", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            this.tagged.clear();
            return;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.NAME_TAG));
        if (slot.isEmpty()) {
            return;
        }
        LivingEntity target = nearestUnnamed(client, player, this.range.value());
        if (target == null) {
            return;
        }
        InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
        InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        if (result.consumesAction()) {
            this.tagged.add(target.getUUID());
            player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private LivingEntity nearestUnnamed(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player || living.hasCustomName()
                    || this.tagged.contains(living.getUUID())) {
                continue;
            }
            double distance = living.distanceToSqr(player);
            if (distance <= rangeSqr && distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }
        return best;
    }
}
