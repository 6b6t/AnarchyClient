package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StatusEffectListSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

public final class NoStatusEffectsModule extends Module {

    private final StatusEffectListSetting effects = this.setting(StatusEffectListSetting.from(StatusEffectListSetting.builder()
            .id("effects")
            .name("Effects")
            .addAllDefaultValue(List.of(
                    MobEffects.BLINDNESS.value(),
                    MobEffects.DARKNESS.value(),
                    MobEffects.LEVITATION.value(),
                    MobEffects.MINING_FATIGUE.value(),
                    MobEffects.NAUSEA.value()
            ))
            .build()));

    public NoStatusEffectsModule() {
        super("no_status_effects", "No Status Effects", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        for (MobEffect effect : this.effects.value()) {
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect));
        }
    }
}
