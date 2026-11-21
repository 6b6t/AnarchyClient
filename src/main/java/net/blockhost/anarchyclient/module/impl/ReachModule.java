package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class ReachModule extends Module {

    private static double blockBonus;
    private static double entityBonus;

    private final NumberSetting block = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("block")
            .name("Block")
            .defaultValue(1.0)
            .min(0.0)
            .max(6.0)
            .step(0.1)
            .build()));
    private final NumberSetting entity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("entity")
            .name("Entity")
            .defaultValue(1.0)
            .min(0.0)
            .max(6.0)
            .step(0.1)
            .build()));

    public ReachModule() {
        super(
                "reach",
                "Reach",
                ModuleCategory.PLAYER,
                "Extends block and entity interaction range by a set number of blocks. This can flag anti-cheats.");
    }

    @Override
    public void tick(final Minecraft client) {
        blockBonus = this.block.value();
        entityBonus = this.entity.value();
    }

    @Override
    protected void onDisable() {
        blockBonus = 0.0;
        entityBonus = 0.0;
    }

    public static double blockBonus() {
        return blockBonus;
    }

    public static double entityBonus() {
        return entityBonus;
    }
}
