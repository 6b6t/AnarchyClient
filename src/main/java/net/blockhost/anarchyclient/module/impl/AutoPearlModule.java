package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public final class AutoPearlModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(40.0)
            .min(5.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private int cooldownTicks;

    public AutoPearlModule() {
        super("auto_pearl", "Auto Pearl", ModuleCategory.COMBAT);
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
        TargetQuery.closest(client.level.entitiesForRendering(), player,
                TargetPolicy.of(true, false, false, true, true, true, true),
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr))
                .ifPresent(target -> {
                    if (SilentHotbar.selectMatching(player, this.id(), stack -> stack.is(Items.ENDER_PEARL),
                            SilentHotbar.PRIORITY_COMBAT, 6, true).isPresent()) {
                        RotationManager.request(new RotationRequest(this.id(),
                                ProjectileAim.rotationToHit(player.getEyePosition(), target, 1.5, 0.03),
                                80, 60.0F, 2, 2.0F, RotationTurnMode.STEPPED, true));
                        RotationManager.apply(player);
                        client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
                        this.cooldownTicks = this.cooldown.value().intValue();
                    }
                });
    }
}
