package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.blockhost.anarchyclient.waypoint.Waypoint;
import net.blockhost.anarchyclient.waypoint.WaypointStore;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        super("waypoints", "Waypoints", ModuleCategory.RENDER, "Renders saved locations with labels and distance cues so routes are easier to follow.");
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
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (Waypoint waypoint : this.waypoints(client)) {
            BlockPos pos = waypoint.pos();
            WorldLineRenderer.Color color = lineColor(waypoint.color());
            AABB box = new AABB(pos).inflate(0.08).move(camera.scale(-1));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, color);
            WorldLineRenderer.lineNoDepth(matrices, submits,
                    new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5).subtract(camera),
                    new Vec3(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5).subtract(camera),
                    color);
        }
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.level == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        for (Waypoint waypoint : this.waypoints(client)) {
            BlockPos pos = waypoint.pos();
            Vec3 labelPos = new Vec3(pos.getX() + 0.5, pos.getY() + 2.3, pos.getZ() + 0.5);
            Vec3 projected = client.gameRenderer.projectPointToScreen(labelPos);
            if (projected.z > 1.0) {
                continue;
            }
            String text = label(waypoint.name(), Math.sqrt(client.player.distanceToSqr(labelPos)));
            int screenX = (int) ((projected.x + 1.0) * 0.5 * graphics.guiWidth());
            int screenY = (int) ((1.0 - projected.y) * 0.5 * graphics.guiHeight());
            int halfWidth = client.font.width(text) / 2;
            graphics.fill(screenX - halfWidth - 2, screenY - 2, screenX + halfWidth + 2, screenY + 9, 0x60000000);
            graphics.text(client.font, text, screenX - halfWidth, screenY, textColor(waypoint.color()));
        }
    }

    private List<Waypoint> waypoints(final Minecraft client) {
        List<Waypoint> waypoints = new ArrayList<>(AnarchyClient.WAYPOINTS.byWorld(WaypointStore.currentWorld(client)));
        waypoints.addAll(this.legacy);
        return waypoints;
    }

    static String label(final String name, final double distance) {
        return name + " " + Math.round(distance) + "m";
    }

    /** Waypoint colors are stored as RGBA. */
    static WorldLineRenderer.Color lineColor(final int rgba) {
        return new WorldLineRenderer.Color(rgba >>> 24, rgba >>> 16 & 0xFF, rgba >>> 8 & 0xFF, rgba & 0xFF);
    }

    /** Opaque ARGB text color for an RGBA waypoint color. */
    static int textColor(final int rgba) {
        return 0xFF000000 | rgba >>> 8;
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
