package net.blockhost.anarchyclient.module.impl;

import java.util.Comparator;
import java.util.Optional;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.projectile.ProjectileAim;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;

public final class BowAimbotModule extends Module {

    // Launch speeds from vanilla BowItem (power * 3.0) and CrossbowItem (3.15 for arrows).
    private static final double BOW_FULL_VELOCITY = 3.0;
    private static final double CROSSBOW_ARROW_VELOCITY = 3.15;
    private static final double CROSSBOW_FIREWORK_VELOCITY = 1.6;

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(8.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));

    public BowAimbotModule() {
        super(
                "bow_aimbot",
                "Bow Aimbot",
                ModuleCategory.COMBAT,
                "Aims at the nearest player or hostile in range while you draw a bow or hold a loaded crossbow, leading the target and compensating for arrow drop.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !isAiming(player)) {
            RotationManager.clear(this.id());
            return;
        }
        TargetPolicy policy =
                TargetPolicy.of(this.players.value(), this.hostiles.value(), false, true, true, true, true);
        Optional<LivingEntity> target = TargetQuery.closest(
                client.level.entitiesForRendering(),
                player,
                policy,
                this.range.value(),
                Comparator.comparingDouble(player::distanceToSqr));
        target.ifPresentOrElse(
                entity -> RotationManager.request(new RotationRequest(
                        this.id(),
                        ProjectileAim.rotationToHit(
                                player.getEyePosition(),
                                entity,
                                arrowVelocity(player.getMainHandItem(), player.getTicksUsingItem()),
                                firesFirework(player.getMainHandItem()) ? 0.0 : ProjectileAim.gravity("bow")),
                        75,
                        30.0F,
                        2,
                        2.0F,
                        RotationTurnMode.STEPPED,
                        true)),
                () -> RotationManager.clear(this.id()));
        // A request does nothing until it is applied; nothing else applies it for this module.
        RotationManager.apply(player);
    }

    static double arrowVelocity(final ItemStack stack, final int ticksUsing) {
        if (firesFirework(stack)) {
            return CROSSBOW_FIREWORK_VELOCITY;
        }
        if (stack.is(Items.CROSSBOW)) {
            return CROSSBOW_ARROW_VELOCITY;
        }
        return BowItem.getPowerForTime(ticksUsing) * BOW_FULL_VELOCITY;
    }

    /** A crossbow loaded with a firework launches it slower than an arrow, and fireworks have no gravity. */
    static boolean firesFirework(final ItemStack stack) {
        return stack.is(Items.CROSSBOW)
                && stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY)
                        .contains(Items.FIREWORK_ROCKET);
    }

    static boolean isAiming(final LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return stack.is(Items.BOW) && player.isUsingItem()
                || stack.is(Items.CROSSBOW) && (player.isUsingItem() || CrossbowItem.isCharged(stack));
    }
}
