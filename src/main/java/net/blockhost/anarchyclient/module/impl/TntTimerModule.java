package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TntTimerModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final BooleanSetting labels = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("labels")
            .name("Labels")
            .defaultValue(true)
            .build()));
    private final BooleanSetting boxes = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("boxes")
            .name("Boxes")
            .defaultValue(true)
            .build()));

    public TntTimerModule() {
        super("tnt_timer", "TNT Timer", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        if (!this.boxes.value()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (player == null || client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof PrimedTnt tnt && tnt.distanceToSqr(player) <= rangeSqr) {
                AABB box = tnt.getBoundingBox().inflate(0.08).move(camera.scale(-1));
                WorldLineRenderer.boxNoDepth(matrices, submits, box, colorForFuse(tnt.getFuse()));
            }
        }
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        LocalPlayer player = client.player;
        if (!this.labels.value()
                || player == null
                || client.level == null
                || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof PrimedTnt tnt) || tnt.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            double x = Mth.lerp(partialTick, tnt.xo, tnt.getX());
            double y = Mth.lerp(partialTick, tnt.yo, tnt.getY()) + tnt.getBoundingBox().getYsize() + 0.35;
            double z = Mth.lerp(partialTick, tnt.zo, tnt.getZ());
            Vec3 projected = client.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
            if (projected.z > 1.0) {
                continue;
            }
            String text = formatFuse(tnt.getFuse());
            int screenX = (int) ((projected.x + 1.0) * 0.5 * graphics.guiWidth());
            int screenY = (int) ((1.0 - projected.y) * 0.5 * graphics.guiHeight());
            int halfWidth = client.font.width(text) / 2;
            graphics.fill(screenX - halfWidth - 2, screenY - 2, screenX + halfWidth + 2, screenY + 9, 0x70000000);
            graphics.text(client.font, text, screenX - halfWidth, screenY, 0xFFFFD166, true);
        }
    }

    static String formatFuse(final int fuseTicks) {
        // Locale.ROOT keeps the decimal point stable regardless of the system locale.
        return String.format(java.util.Locale.ROOT, "%.1fs", Math.max(0, fuseTicks) / 20.0);
    }

    static WorldLineRenderer.Color colorForFuse(final int fuseTicks) {
        if (fuseTicks <= 20) {
            return new WorldLineRenderer.Color(255, 70, 70, 220);
        }
        if (fuseTicks <= 40) {
            return new WorldLineRenderer.Color(255, 190, 80, 210);
        }
        return new WorldLineRenderer.Color(255, 230, 130, 190);
    }
}
