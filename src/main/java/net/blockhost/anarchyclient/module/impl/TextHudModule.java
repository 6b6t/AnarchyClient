package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.setting.StringListSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class TextHudModule extends HudElementModule {

    private final StringListSetting lines = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("lines")
            .name("Lines")
            .addAllDefaultValue(List.of("AnarchyClient"))
            .build()));

    public TextHudModule() {
        super("text_hud", "Text HUD", "Top Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return this.lines.value();
    }
}
