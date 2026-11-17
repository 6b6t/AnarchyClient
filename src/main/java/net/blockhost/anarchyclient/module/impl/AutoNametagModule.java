package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
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
        super("auto_nametag", "Auto Nametag", ModuleCategory.WORLD,
                "Uses a renamed name tag from the hotbar on the nearest unnamed mob within range.");
    }

    @Override
    protected void onDisable() {
        this.tagged.clear();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.tagged.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        // Vanilla only applies a name tag that has been renamed in an anvil.
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), AutoNametagModule::isUsableTag);
        if (slot.isEmpty()) {
            return;
        }
        Mob target = nearestUnnamed(client, player, this.range.value());
        if (target == null) {
            return;
        }
        InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
        InteractionResult result = client.gameMode.interact(player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
        // Remember the mob either way, so a refused tag does not retry the same mob forever.
        this.tagged.add(target.getUUID());
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        }
    }

    static boolean isUsableTag(final ItemStack stack) {
        return stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME);
    }

    private Mob nearestUnnamed(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.entitiesForRendering()) {
            // Name tags work on mobs only, not on players or armor stands.
            if (!(entity instanceof Mob living) || !living.isAlive() || living.hasCustomName()
                    || this.tagged.contains(living.getUUID()) || !CombatTargets.canReachEntity(player, living)) {
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
