package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.projectile.ProjectileAim;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public final class AutoBowModule extends Module {

    private final NumberSetting chargeTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("charge_ticks")
            .name("Charge")
            .defaultValue(18.0)
            .min(3.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(8.0)
            .max(128.0)
            .step(4.0)
            .build()));

    public AutoBowModule() {
        super("auto_bow", "Auto Bow", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !player.getMainHandItem().is(Items.BOW) || !player.isUsingItem()) {
            return;
        }
        TargetQuery.closest(client.level.entitiesForRendering(), player,
                TargetPolicy.of(true, false, false, true, true, true, true),
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr))
                .ifPresent(target -> {
                    RotationManager.request(new RotationRequest(this.id(),
                            ProjectileAim.rotationToHit(player.getEyePosition(), target, 3.0, 0.05),
                            90, 60.0F, 1, 1.0F, RotationTurnMode.STEPPED, true));
                    RotationManager.apply(player);
                    if (player.getTicksUsingItem() >= this.chargeTicks.value()) {
                        client.gameMode.releaseUsingItem(player);
                    }
                });
    }
}
