package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.ColorSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SettingColor;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class SkyboxModule extends Module {

    private static volatile SkyboxModule active;

    private final ColorSetting skyColor = this.setting(ColorSetting.from(ColorSetting.builder()
            .id("sky_color")
            .name("Sky Color")
            .defaultValue(SettingColor.rgb(120, 60, 200))
            .build()));
    private final BooleanSetting rainbow = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rainbow")
            .name("Rainbow")
            .defaultValue(false)
            .description("Cycles the sky through all colors instead of using Sky Color.")
            .build()));
    private final NumberSetting rainbowSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("rainbow_seconds")
            .name("Rainbow Speed")
            .defaultValue(20.0)
            .min(2.0)
            .max(120.0)
            .step(1.0)
            .description("Seconds for one full color cycle.")
            .build()));
    private final BooleanSetting fog = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fog")
            .name("Fog")
            .defaultValue(true)
            .description("Also tints the horizon fog so it blends into the sky.")
            .build()));
    private final BooleanSetting stars = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("stars")
            .name("Stars")
            .defaultValue(false)
            .description("Shows the stars at any time of day.")
            .build()));

    public SkyboxModule() {
        super("skybox", "Skybox", ModuleCategory.RENDER,
                "Replaces the overworld sky color with your own color or a slow rainbow, and can show stars by day. Only you see it.");
        this.skyColor.visibleWhen(() -> !this.rainbow.value());
        this.rainbowSeconds.visibleWhen(this.rainbow::value);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    /** Returns the sky color to render, or {@code null} to keep vanilla's. Called from the sky mixin. */
    public static Vector3fc skyColor() {
        SkyboxModule module = active;
        return module == null ? null : module.color(System.currentTimeMillis());
    }

    /** Returns the fog color to render, or {@code null} to keep vanilla's. Called from the fog mixin. */
    public static Vector3fc fogColor() {
        SkyboxModule module = active;
        return module == null || !module.fog.value() ? null : module.color(System.currentTimeMillis());
    }

    public static boolean forceStars() {
        SkyboxModule module = active;
        return module != null && module.stars.value();
    }

    private Vector3fc color(final long millis) {
        if (this.rainbow.value()) {
            return rainbow(millis, this.rainbowSeconds.value());
        }
        SettingColor color = this.skyColor.value();
        return new Vector3f(color.red() / 255.0F, color.green() / 255.0F, color.blue() / 255.0F);
    }

    static Vector3fc rainbow(final long millis, final double seconds) {
        double period = Math.max(1.0, seconds) * 1000.0;
        float hue = (float) ((millis % (long) period) / period);
        int rgb = Mth.hsvToRgb(hue, 0.6F, 0.9F);
        return new Vector3f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
    }
}
