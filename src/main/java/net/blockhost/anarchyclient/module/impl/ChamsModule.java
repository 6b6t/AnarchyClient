package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChamsModule extends Module {

    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostiles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostiles")
            .name("Hostiles")
            .defaultValue(false)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(128.0)
            .min(8.0)
            .max(512.0)
            .step(8.0)
            .build()));

    public ChamsModule() {
        super("chams", "Chams", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player || entity.distanceToSqr(client.player) > rangeSqr || !this.shouldRender(entity)) {
                continue;
            }
            AABB box = entity.getBoundingBox().inflate(0.05).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(120, 230, 255, 35));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(120, 230, 255, 170));
        }
    }

    private boolean shouldRender(final Entity entity) {
        if (entity instanceof Player) {
            return this.players.value();
        }
        return entity instanceof LivingEntity && TargetClassifier.isHostile(entity) && this.hostiles.value();
    }
}
