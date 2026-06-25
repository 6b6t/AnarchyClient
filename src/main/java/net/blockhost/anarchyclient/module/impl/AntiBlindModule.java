package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;
import net.blockhost.anarchyclient.setting.BooleanSetting;

public final class AntiBlindModule extends Module {

    private final BooleanSetting blindness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blindness")
            .name("Blindness")
            .defaultValue(true)
            .build()));
    private final BooleanSetting darkness = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("darkness")
            .name("Darkness")
            .defaultValue(true)
            .build()));

    public AntiBlindModule() {
        super("anti_blind", "Anti Blind", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        this.updateSuppression();
    }

    @Override
    public void tick(final net.minecraft.client.Minecraft client) {
        this.updateSuppression();
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }

    private void updateSuppression() {
        java.util.List<RenderSuppression.Kind> kinds = new java.util.ArrayList<>();
        if (this.blindness.value()) {
            kinds.add(RenderSuppression.Kind.BLINDNESS);
        }
        if (this.darkness.value()) {
            kinds.add(RenderSuppression.Kind.DARKNESS);
        }
        RenderSuppression.enable(this.id(), kinds.toArray(RenderSuppression.Kind[]::new));
    }
}
