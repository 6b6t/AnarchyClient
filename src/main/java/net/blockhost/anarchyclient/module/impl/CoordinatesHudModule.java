package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;

public final class CoordinatesHudModule extends Module {

    private final SelectSetting corner = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("corner")
            .name("Corner")
            .defaultValue("Top Left")
            .addAllOptions(List.of("Top Left", "Top Right", "Bottom Left", "Bottom Right"))
            .build()));
    private final BooleanSetting showFps = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_fps")
            .name("FPS")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showPing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_ping")
            .name("Ping")
            .defaultValue(true)
            .build()));

    public CoordinatesHudModule() {
        super("coordinates_hud", "Coordinates HUD", ModuleCategory.HUD);
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.screen instanceof AnarchyClientScreen) {
            return;
        }
        List<String> lines = this.lines(client);
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        int x = this.corner.value().endsWith("Right") ? graphics.guiWidth() - width - 6 : 6;
        int y = this.corner.value().startsWith("Bottom") ? graphics.guiHeight() - lines.size() * 10 - 6 : 6;
        graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * 10 + 1, 0x66000000);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(client.font, lines.get(index), x, y + index * 10, 0xFFECE8E0, true);
        }
    }

    List<String> lines(final Minecraft client) {
        List<String> lines = new java.util.ArrayList<>();
        if (client.player == null) {
            return lines;
        }
        lines.add(String.format("XYZ %.1f %.1f %.1f", client.player.getX(), client.player.getY(), client.player.getZ()));
        lines.add(client.player.level().dimension().identifier().toString());
        if (this.showFps.value()) {
            lines.add(client.getFps() + " FPS");
        }
        if (this.showPing.value() && client.getConnection() != null) {
            PlayerInfo info = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (info != null) {
                lines.add(info.getLatency() + " ms");
            }
        }
        return lines;
    }
}
