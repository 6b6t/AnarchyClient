package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class AutoWaspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(24.0)
            .min(4.0)
            .max(128.0)
            .step(2.0)
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.35)
            .min(0.05)
            .max(1.5)
            .step(0.05)
            .build()));

    public AutoWaspModule() {
        super("auto_wasp", "Auto Wasp", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (player == null || target == null) {
            return;
        }
        Vec3 delta = target.position().subtract(player.position()).multiply(1.0, 0.25, 1.0);
        if (delta.lengthSqr() > 1.0E-6) {
            player.setDeltaMovement(delta.normalize().scale(this.speed.value()));
        }
    }
}
