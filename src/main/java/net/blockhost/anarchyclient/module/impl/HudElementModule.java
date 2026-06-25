package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

abstract class HudElementModule extends Module {

    private final SelectSetting corner;
    private final NumberSetting xOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("x_offset")
            .name("X Offset")
            .defaultValue(0.0)
            .min(0.0)
            .max(500.0)
            .step(2.0)
            .build()));
    private final NumberSetting yOffset = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("y_offset")
            .name("Y Offset")
            .defaultValue(0.0)
            .min(0.0)
            .max(500.0)
            .step(2.0)
            .build()));
    private final BooleanSetting background = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("background")
            .name("Background")
            .defaultValue(true)
            .build()));

    protected HudElementModule(final String id, final String name, final String defaultCorner) {
        super(id, name, ModuleCategory.HUD);
        this.corner = this.setting(SelectSetting.from(SelectSetting.builder()
                .id("corner")
                .name("Corner")
                .defaultValue(defaultCorner)
                .addAllOptions(List.of("Top Left", "Top Right", "Bottom Left", "Bottom Right"))
                .build()));
    }

    @Override
    public final void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        HudText.panel(client, graphics, this.lines(client), this.corner.value(),
                this.xOffset.value().intValue(), this.yOffset.value().intValue(), this.color(), this.background.value());
    }

    protected int color() {
        return 0xFFECE8E0;
    }

    protected abstract List<String> lines(Minecraft client);
}
