package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;

public final class HandViewModule extends Module {

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

    HandTransform transform() {
        return new HandTransform(this.hideHand.value(), this.scale.value(), this.x.value(), this.y.value());
    }

    record HandTransform(boolean hidden, double scale, double x, double y) {
    }
}
