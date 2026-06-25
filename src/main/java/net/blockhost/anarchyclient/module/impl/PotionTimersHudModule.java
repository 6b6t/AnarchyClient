package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Comparator;
import java.util.List;

public final class PotionTimersHudModule extends HudElementModule {

    public PotionTimersHudModule() {
        super("potion_timers_hud", "Potion Timers", "Top Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return client.player.getActiveEffects().stream()
                .sorted(Comparator.comparing(effect -> Component.translatable(effect.getDescriptionId()).getString()))
                .map(PotionTimersHudModule::line)
                .toList();
    }

    static String line(final MobEffectInstance effect) {
        String name = Component.translatable(effect.getDescriptionId()).getString();
        String amplifier = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
        return name + amplifier + " " + formatTicks(effect.getDuration());
    }

    static String formatTicks(final int ticks) {
        if (ticks < 0 || ticks >= 20 * 60 * 60) {
            return "inf";
        }
        int seconds = Math.max(0, ticks / 20);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
