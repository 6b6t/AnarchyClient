package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrailModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(32.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting lifetime = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime")
            .name("Lifetime")
            .defaultValue(160.0)
            .min(20.0)
            .max(600.0)
            .step(20.0)
            .build()));
    private final Map<UUID, List<TrailPoint>> trails = new HashMap<>();

    public TrailModule() {
        super("trail", "Trail", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.trails.clear();
            return;
        }
        for (List<TrailPoint> points : this.trails.values()) {
            points.replaceAll(TrailPoint::tick);
            points.removeIf(point -> point.age() > this.lifetime.value().intValue());
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == player || target.distanceToSqr(player) > rangeSqr) {
                continue;
            }
            List<TrailPoint> points = this.trails.computeIfAbsent(target.getUUID(), ignored -> new ArrayList<>());
            Vec3 position = target.position().add(0.0, 0.08, 0.0);
            if (points.isEmpty() || points.getLast().position().distanceToSqr(position) > 0.2) {
                points.add(new TrailPoint(position, 0));
            }
        }
        this.trails.values().removeIf(List::isEmpty);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color = new WorldLineRenderer.Color(255, 210, 92, 170);
        for (List<TrailPoint> points : this.trails.values()) {
            Iterator<TrailPoint> iterator = points.iterator();
            if (!iterator.hasNext()) {
                continue;
            }
            TrailPoint previous = iterator.next();
            while (iterator.hasNext()) {
                TrailPoint next = iterator.next();
                WorldLineRenderer.lineNoDepth(matrices, submits, previous.position().subtract(camera),
                        next.position().subtract(camera), color);
                previous = next;
            }
        }
    }

    record TrailPoint(Vec3 position, int age) {

        TrailPoint tick() {
            return new TrailPoint(this.position, this.age + 1);
        }
    }
}
