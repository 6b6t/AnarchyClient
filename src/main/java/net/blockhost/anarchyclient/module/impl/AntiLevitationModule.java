package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;

public final class AntiLevitationModule extends Module {

    private final BooleanSetting removeEffect = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("remove_effect")
            .name("Remove")
            .defaultValue(true)
            .build()));

    public AntiLevitationModule() {
        super("anti_levitation", "Anti Levitation", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.hasEffect(MobEffects.LEVITATION)) {
            return;
        }
        if (this.removeEffect.value()) {
            player.removeEffect(MobEffects.LEVITATION);
        } else if (player.getDeltaMovement().y > 0.0) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
        }
    }
}
