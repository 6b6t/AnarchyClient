package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends Module {

    private final NumberSetting gamma = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("gamma")
            .name("Gamma")
            .defaultValue(15.0)
            .min(1.0)
            .max(25.0)
            .step(1.0)
            .build()));
    private Double previousGamma;

    public FullbrightModule() {
        super("fullbright", "Fullbright", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.previousGamma == null) {
            this.previousGamma = client.options.gamma().get();
        }
        client.options.gamma().set(this.gamma.value());
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.previousGamma != null) {
            client.options.gamma().set(this.previousGamma);
            this.previousGamma = null;
        }
    }
}
