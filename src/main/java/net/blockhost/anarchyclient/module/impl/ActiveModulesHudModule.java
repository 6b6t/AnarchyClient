package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.ui.HudEditorScreen;
import net.blockhost.anarchyclient.ui.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Comparator;
import java.util.List;

public final class ActiveModulesHudModule extends Module {

    private final ModuleManager modules;

    public ActiveModulesHudModule(final ModuleManager modules) {
        super("active_modules_hud", "Active Modules", ModuleCategory.HUD);
        this.modules = modules;
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || HudEditorScreen.suppressed(client)) {
            return;
        }
        List<String> lines = this.modules.all().stream()
                .filter(module -> module.enabled() && module != this)
                .sorted(Comparator.comparing(Module::name))
                .map(Module::name)
                .toList();
        if (lines.isEmpty()) {
            return;
        }
        int[] size = HudText.size(client, lines);
        int[] origin = HudLayout.origin(this.id(), this.name(), size[0], size[1], "Top Right", graphics);
        HudText.panelAt(client, graphics, lines, origin[0], origin[1], 0xFF8EEAD5, true);
    }
}
