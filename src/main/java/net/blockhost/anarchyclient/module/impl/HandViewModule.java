package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;

public final class HandViewModule extends Module {

    private static final HandTransform DEFAULT_TRANSFORM = new HandTransform(false, 1.0, 0.0, 0.0);
    private static HandTransform activeTransform = DEFAULT_TRANSFORM;

    private final BooleanSetting hideHand = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hide_hand")
            .name("Hide Hand")
            .defaultValue(false)
            .build()));
    private final NumberSetting scale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("scale")
            .name("Scale")
            .defaultValue(1.0)
            .min(0.1)
            .max(3.0)
            .step(0.05)
            .build()));
    private final NumberSetting x = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("x")
            .name("X")
            .defaultValue(0.0)
            .min(-2.0)
            .max(2.0)
            .step(0.05)
            .build()));
    private final NumberSetting y = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("y")
            .name("Y")
            .defaultValue(0.0)
            .min(-2.0)
            .max(2.0)
            .step(0.05)
            .build()));

    public HandViewModule() {
        super("hand_view", "Hand View", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final net.minecraft.client.Minecraft client) {
        activeTransform = this.transform();
    }

    @Override
    protected void onEnable() {
        activeTransform = this.transform();
    }

    @Override
    protected void onDisable() {
        activeTransform = DEFAULT_TRANSFORM;
    }

    public static HandTransform activeTransform() {
        return activeTransform;
    }

    private HandTransform transform() {
        return new HandTransform(this.hideHand.value(), this.scale.value(), this.x.value(), this.y.value());
    }

    public record HandTransform(boolean hidden, double scale, double x, double y) {

        public boolean identity() {
            return !this.hidden && this.scale == 1.0 && this.x == 0.0 && this.y == 0.0;
        }
    }
}
