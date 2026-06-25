package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;

public final class HitboxModule extends Module {

    private static double activeExpansion;

    private final NumberSetting expansion = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("expansion")
            .name("Expansion")
            .defaultValue(0.4)
            .min(0.0)
            .max(3.0)
            .step(0.05)
            .build()));

    public HitboxModule() {
        super("hitbox", "Hitbox", ModuleCategory.COMBAT, java.util.List.of("hitboxes"));
    }

    @Override
    public void tick(final net.minecraft.client.Minecraft client) {
        activeExpansion = this.expansion.value();
    }

    @Override
    protected void onDisable() {
        activeExpansion = 0.0;
    }

    public static double rangeBonus() {
        return activeExpansion;
    }
}
