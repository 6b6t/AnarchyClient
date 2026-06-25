package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class BoostModule extends Module {

    private final NumberSetting strength = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(2.5)
            .min(0.2)
            .max(10.0)
            .step(0.1)
            .build()));
    private final BooleanSetting autoBoost = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_boost")
            .name("Auto")
            .defaultValue(false)
            .build()));
    private final NumberSetting intervalTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval_ticks")
            .name("Interval")
            .defaultValue(20.0)
            .min(1.0)
            .max(120.0)
            .step(1.0)
            .build()));
    private int timer;

    public BoostModule() {
        super("boost", "Boost", ModuleCategory.MOVEMENT);
        this.intervalTicks.visibleWhen(this.autoBoost::value);
    }

    @Override
    protected void onEnable() {
        this.timer = this.intervalTicks.value().intValue();
        if (!this.autoBoost.value()) {
            this.boost(Minecraft.getInstance());
            this.enabled(false);
        }
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.autoBoost.value()) {
            return;
        }
        if (this.timer > 0) {
            this.timer--;
            return;
        }
        this.boost(client);
        this.timer = Math.max(1, this.intervalTicks.value().intValue());
    }

    private void boost(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null) {
            return;
        }
        Vec3 velocity = boostVector(player.getViewVector(0.0F), this.strength.value());
        player.push(velocity.x, velocity.y, velocity.z);
    }

    static Vec3 boostVector(final Vec3 look, final double strength) {
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.horizontalDistanceSqr() < 1.0E-4) {
            return Vec3.ZERO;
        }
        return horizontal.normalize().scale(strength);
    }
}
