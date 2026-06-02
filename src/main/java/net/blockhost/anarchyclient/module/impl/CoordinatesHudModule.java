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
    private final BooleanSetting showLinkedCoords = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_linked_coords")
            .name("Linked XYZ")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showSpeed = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_speed")
            .name("Speed")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showFacing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_facing")
            .name("Facing")
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
        HudText.panel(client, graphics, this.lines(client), this.corner.value(), 0xFFECE8E0);
    }

    List<String> lines(final Minecraft client) {
        List<String> lines = new java.util.ArrayList<>();
        if (client.player == null) {
            return lines;
        }
        lines.add(String.format("XYZ %.1f %.1f %.1f", client.player.getX(), client.player.getY(), client.player.getZ()));
        if (this.showLinkedCoords.value()) {
            double scale = client.player.level().dimension() == net.minecraft.world.level.Level.NETHER ? 8.0 : 0.125;
            String label = client.player.level().dimension() == net.minecraft.world.level.Level.NETHER ? "OW" : "Nether";
            lines.add(String.format("%s %.1f %.1f", label, client.player.getX() * scale, client.player.getZ() * scale));
        }
        lines.add(client.player.level().dimension().identifier().toString());
        if (this.showFacing.value()) {
            lines.add("Facing " + client.player.getDirection().getName());
        }
        if (this.showSpeed.value()) {
            double speed = client.player.getDeltaMovement().horizontalDistance() * 20.0;
            lines.add(String.format("%.2f b/s", speed));
        }
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
