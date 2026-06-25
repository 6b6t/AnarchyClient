package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ElytraBoostModule extends Module {

    private final NumberSetting strength = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("strength")
            .name("Strength")
            .defaultValue(0.08)
            .min(0.01)
            .max(0.5)
            .step(0.01)
            .build()));

    public ElytraBoostModule() {
        super("elytra_boost", "Elytra Boost", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isFallFlying() || !client.options.keyUp.isDown()) {
            return;
        }
        Vec3 boost = player.getViewVector(0.0F).normalize().scale(this.strength.value());
        player.setDeltaMovement(player.getDeltaMovement().add(boost));
    }
}
