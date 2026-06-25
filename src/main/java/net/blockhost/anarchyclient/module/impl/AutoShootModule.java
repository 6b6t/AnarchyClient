package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.projectile.ProjectileAim;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AutoShootModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(24.0)
            .min(4.0)
            .max(128.0)
            .step(2.0)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(12.0)
            .min(1.0)
            .max(200.0)
            .step(1.0)
            .build()));
    private final SelectSetting projectile = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("projectile")
            .name("Projectile")
            .defaultValue("Throwables")
            .addAllOptions(List.of("Throwables", "Ender Pearl", "Fishing Rod", "All"))
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
    private int cooldownTicks;

    public AutoShootModule() {
        super("auto_shoot", "Auto Shoot", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks-- > 0) {
            return;
        }
        TargetPolicy policy = TargetPolicy.of(this.players.value(), this.hostiles.value(), false,
                true, true, true, true);
        Optional<LivingEntity> target = TargetQuery.closest(client.level.entitiesForRendering(), player, policy,
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr));
        if (target.isEmpty()) {
            return;
        }
        if (SilentHotbar.selectMatching(player, this.id(), stack -> this.matchesProjectile(stack),
                SilentHotbar.PRIORITY_COMBAT, 6, true).isEmpty()) {
            return;
        }
        RotationManager.request(new RotationRequest(
                this.id(),
                ProjectileAim.rotationToHit(player.getEyePosition(), target.orElseThrow(), 1.5, 0.03),
                80,
                60.0F,
                2,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        ));
        RotationManager.apply(player);
        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        this.cooldownTicks = this.cooldown.value().intValue();
    }

    static boolean isThrowable(final ItemStack stack) {
        return stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.EXPERIENCE_BOTTLE);
    }

    private boolean matchesProjectile(final ItemStack stack) {
        return switch (this.projectile.value()) {
            case "Ender Pearl" -> stack.is(Items.ENDER_PEARL);
            case "Fishing Rod" -> stack.is(Items.FISHING_ROD);
            case "All" -> isThrowable(stack) || stack.is(Items.FISHING_ROD);
            default -> isThrowable(stack);
        };
    }
}
