package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BreadcrumbsModule extends Module {

    private final NumberSetting lifetime = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime")
            .name("Lifetime")
            .defaultValue(240.0)
            .min(20.0)
            .max(1200.0)
            .step(20.0)
            .build()));
    private final NumberSetting maxPoints = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_points")
            .name("Points")
            .defaultValue(180.0)
            .min(16.0)
            .max(600.0)
            .step(8.0)
            .build()));
    private final NumberSetting minDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_distance")
            .name("Spacing")
            .defaultValue(0.35)
            .min(0.05)
            .max(2.0)
            .step(0.05)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(190.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final List<TrailPoint> points = new ArrayList<>();

    public BreadcrumbsModule() {
        super("breadcrumbs", "Breadcrumbs", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.points.clear();
            return;
        }
        this.points.replaceAll(TrailPoint::tick);
        this.points.removeIf(point -> point.age() > this.lifetime.value().intValue());

        Vec3 position = player.position().add(0.0, 0.08, 0.0);
        TrailPoint last = this.points.isEmpty() ? null : this.points.getLast();
        double minDistanceSqr = this.minDistance.value() * this.minDistance.value();
        if (last == null || last.position().distanceToSqr(position) >= minDistanceSqr) {
            this.points.add(new TrailPoint(position, 0));
        }
        while (this.points.size() > this.maxPoints.value().intValue()) {
            this.points.removeFirst();
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null || this.points.size() < 2) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color = new WorldLineRenderer.Color(0, 212, 170, this.opacity.value().intValue());
        for (int index = 1; index < this.points.size(); index++) {
            Vec3 start = this.points.get(index - 1).position().subtract(camera);
            Vec3 end = this.points.get(index).position().subtract(camera);
            WorldLineRenderer.lineNoDepth(matrices, submits, start, end, color);
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.points.clear();
    }

    @Override
    protected void onDisable() {
        this.points.clear();
    }

    static TrailPoint tick(final TrailPoint point) {
        return point.tick();
    }

    record TrailPoint(Vec3 position, int age) {

        TrailPoint tick() {
            return new TrailPoint(this.position, this.age + 1);
        }
    }
}
