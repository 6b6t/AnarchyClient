package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.ui.HudEditorScreen;
import net.blockhost.anarchyclient.ui.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

abstract class HudElementModule extends Module {

    // Position (corner + offset) is now owned by the HUD editor, not per-module settings. The default
    // corner only decides where the element sits until the player first drags it.
    private final String defaultCorner;
    private final BooleanSetting background = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("background")
            .name("Background")
            .defaultValue(true)
            .build()));

    protected HudElementModule(final String id, final String name, final String defaultCorner) {
        super(id, name, ModuleCategory.HUD);
        this.defaultCorner = defaultCorner;
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || HudEditorScreen.suppressed(client)) {
            return;
        }
        this.renderHudElement(client, graphics);
    }

    protected void renderHudElement(final Minecraft client, final GuiGraphicsExtractor graphics) {
        List<String> lines = this.lines(client);
        if (lines.isEmpty()) {
            return;
        }
        int[] size = HudText.size(client, lines);
        HudPosition position = this.position(graphics, size[0], size[1]);
        HudText.panelAt(client, graphics, lines, position.x(), position.y(), this.color(), this.background.value());
    }

    protected int color() {
        return 0xFFECE8E0;
    }

    protected HudPosition position(final GuiGraphicsExtractor graphics, final int width, final int height) {
        int[] origin = HudLayout.origin(this.id(), this.name(), width, height, this.defaultCorner, graphics);
        return new HudPosition(origin[0], origin[1]);
    }

    protected abstract List<String> lines(Minecraft client);

    protected record HudPosition(int x, int y) {
    }
}
