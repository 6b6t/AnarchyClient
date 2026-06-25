package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.waypoint.Waypoint;
import net.blockhost.anarchyclient.waypoint.WaypointStore;
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
            .name("Legacy Points")
            .defaultValue("")
            .build()));
    private String lastPoints = "";
    private String lastWorld = "";
    private List<Waypoint> legacy = List.of();

    public WaypointsModule() {
        super("waypoints", "Waypoints", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        String world = WaypointStore.currentWorld(client);
        if (!this.lastPoints.equals(this.points.value()) || !this.lastWorld.equals(world)) {
            this.legacy = parse(world, this.points.value());
            this.lastPoints = this.points.value();
            this.lastWorld = world;
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
        List<Waypoint> waypoints = new ArrayList<>(AnarchyClient.WAYPOINTS.byWorld(WaypointStore.currentWorld(client)));
        waypoints.addAll(this.legacy);
        for (Waypoint waypoint : waypoints) {
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
        return parse("unknown", value);
    }

    static List<Waypoint> parse(final String world, final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return WaypointStore.parseLegacy(world, value);
    }
}
