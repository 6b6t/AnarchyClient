package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.blockhost.anarchyclient.target.TargetPolicy;
import net.blockhost.anarchyclient.target.TargetQuery;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class NametagsModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final BooleanSetting aboveHead = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("above_head")
            .name("Above Head")
            .defaultValue(false)
            .build()));
    private final NumberSetting maxRows = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_rows")
            .name("Rows")
            .defaultValue(8.0)
            .min(1.0)
            .max(24.0)
            .step(1.0)
            .build()));
    private final SelectSetting corner = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("corner")
            .name("Corner")
            .defaultValue("Top Right")
            .addAllOptions(List.of("Top Left", "Top Right", "Bottom Left", "Bottom Right"))
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(false)
            .build()));

    public NametagsModule() {
        super("nametags", "Nametags", ModuleCategory.RENDER);
        this.maxRows.visibleWhen(() -> !this.aboveHead.value());
        this.corner.visibleWhen(() -> !this.aboveHead.value());
    }

    @Override
    protected void onEnable() {
        RenderedEntityCache.subscribe(this.id());
    }

    @Override
    protected void onDisable() {
        RenderedEntityCache.unsubscribe(this.id());
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.level == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        TargetPolicy policy = TargetPolicy.of(true, false, false, false, this.ignoreFriends.value(), false, false);
        if (this.aboveHead.value()) {
            float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            TargetQuery.livingTargets(RenderedEntityCache.entities(), client.player, policy)
                    .filter(Player.class::isInstance)
                    .filter(entity -> entity.distanceToSqr(client.player) <= rangeSqr)
                    .forEach(entity -> this.renderAboveHead(client, graphics, entity, partialTick));
        } else {
            List<String> lines = TargetQuery.livingTargets(RenderedEntityCache.entities(), client.player, policy)
                    .filter(Player.class::isInstance)
                    .filter(entity -> entity.distanceToSqr(client.player) <= rangeSqr)
                    .sorted(Comparator.comparingDouble(client.player::distanceToSqr))
                    .limit(this.maxRows.value().longValue())
                    .map(entity -> this.line(client.player, entity))
                    .toList();
            HudText.panel(client, graphics, lines, this.corner.value(), 0xFFECE8E0);
        }
    }

    private void renderAboveHead(final Minecraft client, final GuiGraphicsExtractor graphics, final LivingEntity entity,
                                 final float partialTick) {
        double x = Mth.lerp(partialTick, entity.xo, entity.getX());
        double y = Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBoundingBox().getYsize() + 0.5;
        double z = Mth.lerp(partialTick, entity.zo, entity.getZ());
        Vec3 projected = client.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
        if (projected.z > 1.0) {
            return;
        }

        String text = this.line(client.player, entity);
        int screenX = (int) ((projected.x + 1.0) * 0.5 * graphics.guiWidth());
        int screenY = (int) ((1.0 - projected.y) * 0.5 * graphics.guiHeight());
        int halfWidth = client.font.width(text) / 2;
        graphics.fill(screenX - halfWidth - 2, screenY - 2, screenX + halfWidth + 2, screenY + 9, 0x60000000);
        graphics.text(client.font, text, screenX - halfWidth, screenY, 0xFFECE8E0);
    }

    private String line(final Player player, final LivingEntity entity) {
        String team = entity.getTeam() == null ? "" : "[" + entity.getTeam().getName() + "] ";
        return "%s%s %.0fm %.1fh".formatted(team, entity.getScoreboardName(), Math.sqrt(entity.distanceToSqr(player)), entity.getHealth());
    }
}
