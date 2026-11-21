package net.blockhost.anarchyclient.module.impl;

import java.time.LocalTime;
import java.util.List;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class WelcomerHudModule extends HudElementModule {

    private final BooleanSetting timeGreeting = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("time_greeting")
            .name("Time Greeting")
            .defaultValue(true)
            .description("Greets you by the time of day instead of using the text below.")
            .build()));
    private final StringSetting text = this.setting(StringSetting.from(StringSetting.builder()
            .id("text")
            .name("Text")
            .defaultValue("Welcome, {name}")
            .description("The greeting. {name} is replaced with your username.")
            .build()));

    public WelcomerHudModule() {
        super("welcomer_hud", "Welcomer HUD", "Shows a greeting with your username on the HUD.", "Top Left");
        this.text.visibleWhen(() -> !this.timeGreeting.value());
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        String name = client.getUser().getName();
        String greeting = this.timeGreeting.value()
                ? timeGreeting(LocalTime.now()) + ", " + name
                : this.text.value().replace("{name}", name);
        return greeting.isBlank() ? List.of() : List.of(greeting);
    }

    static String timeGreeting(final LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        }
        if (hour >= 12 && hour < 18) {
            return "Good afternoon";
        }
        if (hour >= 18 && hour < 22) {
            return "Good evening";
        }
        return "Good night";
    }
}
