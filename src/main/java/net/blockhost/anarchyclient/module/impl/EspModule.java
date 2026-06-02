package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class EspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting hostileMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hostile_mobs")
            .name("Hostiles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting passiveMobs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("passive_mobs")
            .name("Passives")
            .defaultValue(false)
            .build()));
    private final BooleanSetting lineOfSightFade = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("line_of_sight_fade")
            .name("LOS Fade")
            .defaultValue(true)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(170.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final SelectSetting colorMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("color_mode")
            .name("Color")
            .defaultValue("Type")
            .addAllOptions(List.of("Type", "Distance", "Aqua"))
            .build()));

    public EspModule() {
        super("esp", "ESP", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.gameRenderer.getMainCamera().entity() instanceof Player cameraPlayer ? cameraPlayer : null;
        PoseStack matrices = context.poseStack();
        MultiBufferSource consumers = context.bufferSource();
        if (client.level == null || player == null || matrices == null || consumers == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.getMainCamera().position();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!this.shouldRender(entity, player)) {
                continue;
            }
            double distanceSqr = entity.distanceToSqr(player);
            double rangeValue = this.range.value();
            if (distanceSqr > rangeValue * rangeValue) {
                continue;
            }

            int alpha = this.opacity.value().intValue();
            if (this.lineOfSightFade.value() && !player.hasLineOfSight(entity)) {
                alpha = Math.max(35, alpha / 2);
            }

            AABB box = entity.getBoundingBox().inflate(0.04).move(camera.scale(-1));
            WorldLineRenderer.box(matrices, consumers, box, this.color(entity, Math.sqrt(distanceSqr), alpha));
        }
    }

    boolean shouldRender(final Entity entity, final Player player) {
        if (!EntityTargeting.isValidLivingTarget(entity, player)) {
            return false;
        }
        return this.players.value() && EntityTargeting.isPlayer(entity)
                || this.hostileMobs.value() && EntityTargeting.isHostile(entity)
                || this.passiveMobs.value() && EntityTargeting.isPassive(entity);
    }

    private WorldLineRenderer.Color color(final Entity entity, final double distance, final int alpha) {
        return switch (this.colorMode.value()) {
            case "Distance" -> {
                int red = (int) Math.min(255, distance * 3);
                int green = Math.max(80, 255 - red);
                yield new WorldLineRenderer.Color(red, green, 80, alpha);
            }
            case "Aqua" -> new WorldLineRenderer.Color(0, 212, 170, alpha);
            default -> {
                if (EntityTargeting.isPlayer(entity)) {
                    yield new WorldLineRenderer.Color(0, 212, 170, alpha);
                }
                if (EntityTargeting.isHostile(entity)) {
                    yield new WorldLineRenderer.Color(255, 86, 86, alpha);
                }
                yield new WorldLineRenderer.Color(245, 205, 92, alpha);
            }
        };
    }
}
