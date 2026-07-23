package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.blockhost.anarchyclient.ui.HudEditorScreen;
import net.blockhost.anarchyclient.ui.HudLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class RadarHudModule extends Module {

    private final NumberSetting size = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("size")
            .name("Size")
            .defaultValue(120.0)
            .min(72.0)
            .max(220.0)
            .step(4.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(16.0)
            .max(512.0)
            .step(8.0)
            .build()));
    private final NumberSetting dotSize = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("dot_size")
            .name("Dot")
            .defaultValue(3.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotateWithPlayer = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate_with_player")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showPlayers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showHostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_hostiles")
            .name("Hostiles")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showPassives = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_passives")
            .name("Passives")
            .defaultValue(false)
            .build()));
    private final BooleanSetting showItems = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_items")
            .name("Items")
            .defaultValue(false)
            .build()));

    public RadarHudModule() {
        super("radar_hud", "Radar HUD", ModuleCategory.HUD);
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || HudEditorScreen.suppressed(client)) {
            return;
        }

        int panelSize = this.size.value().intValue();
        int[] origin = HudLayout.origin(this.id(), this.name(), panelSize, panelSize, "Top Left", graphics);
        int x = origin[0];
        int y = origin[1];
        int centerX = x + panelSize / 2;
        int centerY = y + panelSize / 2;

        graphics.fill(x, y, x + panelSize, y + panelSize, 0x66000000);
        graphics.fill(centerX, y + 4, centerX + 1, y + panelSize - 4, 0x33FFFFFF);
        graphics.fill(x + 4, centerY, x + panelSize - 4, centerY + 1, 0x33FFFFFF);
        graphics.fill(centerX - 2, centerY - 2, centerX + 3, centerY + 3, 0xFFECE8E0);

        double rangeValue = this.range.value();
        double rangeSqr = rangeValue * rangeValue;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == player || entity.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            int color = this.entityColor(entity);
            if (color == 0) {
                continue;
            }
            RadarPoint point = this.point(player, entity, panelSize, rangeValue);
            int dot = this.dotSize.value().intValue();
            graphics.fill(centerX + point.x() - dot, centerY + point.y() - dot,
                    centerX + point.x() + dot + 1, centerY + point.y() + dot + 1, color);
        }

        String label = this.rotateWithPlayer.value() ? "^" : "N";
        graphics.text(client.font, label, centerX - client.font.width(label) / 2, y + 3, 0xFFECE8E0, true);
    }

    private RadarPoint point(final LocalPlayer player, final Entity entity, final int panelSize, final double rangeValue) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        if (this.rotateWithPlayer.value()) {
            double yaw = Math.toRadians(player.getYRot());
            double rotatedX = dx * Math.cos(yaw) - dz * Math.sin(yaw);
            double rotatedZ = dx * Math.sin(yaw) + dz * Math.cos(yaw);
            dx = rotatedX;
            dz = rotatedZ;
        }
        double scale = (panelSize / 2.0 - 8.0) / rangeValue;
        int x = (int) Math.round(clamp(dx * scale, -panelSize / 2.0 + 8.0, panelSize / 2.0 - 8.0));
        int y = (int) Math.round(clamp(dz * scale, -panelSize / 2.0 + 8.0, panelSize / 2.0 - 8.0));
        return new RadarPoint(x, y);
    }

    private int entityColor(final Entity entity) {
        if (entity instanceof Player) {
            return this.showPlayers.value() ? 0xFF8EEAD5 : 0;
        }
        if (entity instanceof ItemEntity) {
            return this.showItems.value() ? 0xFFE6C76E : 0;
        }
        if (entity instanceof LivingEntity && TargetClassifier.isHostile(entity)) {
            return this.showHostiles.value() ? 0xFFE56A6A : 0;
        }
        if (entity instanceof LivingEntity && TargetClassifier.isPassive(entity)) {
            return this.showPassives.value() ? 0xFF86D978 : 0;
        }
        return 0;
    }

    private static double clamp(final double value, final double min, final double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RadarPoint(int x, int y) {
    }
}
