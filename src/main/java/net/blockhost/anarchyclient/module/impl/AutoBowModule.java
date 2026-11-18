package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.projectile.ProjectileAim;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.Optional;

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

    private Rotation lastAim;

    public AutoBowModule() {
        super("auto_bow", "Auto Bow", ModuleCategory.COMBAT,
                "Aims your drawn bow at the closest target with projectile drop compensation and releases it once charged.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null
                || !player.getMainHandItem().is(Items.BOW) || !player.isUsingItem()) {
            this.lastAim = null;
            return;
        }
        Optional<LivingEntity> target = TargetQuery.closest(client.level.entitiesForRendering(), player,
                TargetPolicy.of(true, false, false, true, true, true, true),
                this.range.value(), Comparator.comparingDouble(player::distanceToSqr));
        if (target.isEmpty()) {
            this.lastAim = null;
            return;
        }
        Rotation aim = ProjectileAim.rotationToHit(player.getEyePosition(), target.get(), 3.0, 0.05);
        // The release packet has no rotation: the arrow flies where the server last saw us look, which is the
        // aim requested last tick. Comparing with this tick's aim would never match a strafing target.
        if (player.getTicksUsingItem() >= this.chargeTicks.value() && this.lastAim != null
                && AutoShootModule.isAimed(RotationManager.serverRotation(player), this.lastAim)) {
            client.gameMode.releaseUsingItem(player);
            this.lastAim = null;
            return;
        }
        RotationManager.request(new RotationRequest(this.id(), aim,
                90, 60.0F, 1, 1.0F, RotationTurnMode.STEPPED, true));
        RotationManager.apply(player);
        this.lastAim = aim;
    }

    @Override
    protected void onDisable() {
        this.lastAim = null;
    }
}
