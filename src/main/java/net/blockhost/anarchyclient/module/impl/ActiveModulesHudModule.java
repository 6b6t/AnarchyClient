package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Comparator;
import java.util.List;

public final class ActiveModulesHudModule extends Module {

    private final ModuleManager modules;
    private final SelectSetting corner = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("corner")
            .name("Corner")
            .defaultValue("Top Right")
            .addAllOptions(List.of("Top Left", "Top Right", "Bottom Left", "Bottom Right"))
            .build()));

    public ActiveModulesHudModule(final ModuleManager modules) {
        super("active_modules_hud", "Active Modules", ModuleCategory.HUD);
        this.modules = modules;
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.screen instanceof AnarchyClientScreen) {
            return;
        }
        List<String> lines = this.modules.all().stream()
                .filter(module -> module.enabled() && module != this)
                .sorted(Comparator.comparing(Module::name))
                .map(Module::name)
                .toList();
        HudText.panel(client, graphics, lines, this.corner.value(), 0xFF8EEAD5);
    }
}
