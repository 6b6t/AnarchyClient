package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class AnimationsModule extends Module {

    private static AnimationsModule active;

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Swing")
            .addAllOptions(List.of("Swing", "Slide", "Spin", "None"))
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(1.0)
            .min(0.1)
            .max(5.0)
            .step(0.1)
            .build()));
    private final NumberSetting intensity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("intensity")
            .name("Intensity")
            .defaultValue(0.25)
            .min(0.0)
            .max(1.0)
            .step(0.05)
            .build()));

    public AnimationsModule() {
        super("animations", "Animations", ModuleCategory.RENDER,
                "Applies a looping swing, slide, or spin motion to your held item, with adjustable speed and intensity.");
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

    /**
     * The held-item transform for this frame. Computed at render time with the partial tick, so the motion is
     * smooth at any frame rate instead of stepping 20 times per second.
     */
    public static AnimationTransform activeTransform(final float partialTick) {
        AnimationsModule module = active;
        Minecraft client = Minecraft.getInstance();
        if (module == null || client.player == null) {
            return AnimationTransform.identity();
        }
        return transform(module.mode.value(), client.player.tickCount + partialTick, module.speed.value(),
                module.intensity.value());
    }

    static AnimationTransform transform(final String mode, final double ticks, final double speed,
                                        final double amount) {
        double phase = ticks * 0.18 * speed;
        return switch (mode) {
            case "None" -> AnimationTransform.identity();
            case "Slide" -> new AnimationTransform(Math.sin(phase) * amount * 0.35, 0.0, 0.0, 0.0, 0.0,
                    Math.sin(phase) * amount * 12.0);
            case "Spin" -> new AnimationTransform(0.0, 0.0, 0.0, 0.0, phase * 38.0 * amount, 0.0);
            default -> new AnimationTransform(Math.sin(phase) * amount * 0.08, Math.cos(phase) * amount * 0.05, 0.0,
                    Math.sin(phase) * amount * 8.0, 0.0, Math.cos(phase) * amount * 10.0);
        };
    }

    public record AnimationTransform(double x, double y, double z, double pitch, double yaw, double roll) {

        static AnimationTransform identity() {
            return new AnimationTransform(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public boolean identityTransform() {
            return this.x == 0.0 && this.y == 0.0 && this.z == 0.0
                    && this.pitch == 0.0 && this.yaw == 0.0 && this.roll == 0.0;
        }
    }
}
