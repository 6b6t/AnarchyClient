package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.minecraft.client.Minecraft;

public final class TextHudModule extends HudElementModule {

    private final StringListSetting lines = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("lines")
            .name("Lines")
            .addAllDefaultValue(List.of("AnarchyClient"))
            .build()));

    public TextHudModule() {
        super(
                "text_hud",
                "Text HUD",
                "Shows lines of custom text that you write on the HUD, one entry per line.",
                "Top Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return this.lines.value();
    }
}
