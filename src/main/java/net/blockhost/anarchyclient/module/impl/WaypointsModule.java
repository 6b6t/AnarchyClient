package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class WaypointsModule extends Module {

    private final StringSetting points = this.setting(StringSetting.from(StringSetting.builder()
            .id("points")
            .name("Points")
            .defaultValue("home:0:64:0")
            .build()));
    private String lastPoints = "";
    private List<Waypoint> parsed = List.of();

    public WaypointsModule() {
        super("waypoints", "Waypoints", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.lastPoints.equals(this.points.value())) {
            this.parsed = parse(this.points.value());
            this.lastPoints = this.points.value();
        }
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (Waypoint waypoint : this.parsed) {
            BlockPos pos = waypoint.pos();
            AABB box = new AABB(pos).inflate(0.08).move(camera.scale(-1));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(120, 230, 255, 210));
            WorldLineRenderer.lineNoDepth(matrices, submits,
                    new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5).subtract(camera),
                    new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5).subtract(camera),
                    new WorldLineRenderer.Color(120, 230, 255, 160));
        }
    }

    static List<Waypoint> parse(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Waypoint> result = new ArrayList<>();
        for (String token : value.split(";")) {
            String[] parts = token.trim().split(":");
            if (parts.length != 4) {
                continue;
            }
            try {
                result.add(new Waypoint(parts[0], new BlockPos(
                        Integer.parseInt(parts[1].trim()),
                        Integer.parseInt(parts[2].trim()),
                        Integer.parseInt(parts[3].trim())
                )));
            } catch (NumberFormatException ignored) {
            }
        }
        return List.copyOf(result);
    }

    record Waypoint(String name, BlockPos pos) {
    }
}
