package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.RenderedEntityCache;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class SkeletonEspModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(96.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(210.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final BooleanSetting invisibles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invisibles")
            .name("Invisibles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(false)
            .build()));

    public SkeletonEspModule() {
        super("skeleton_esp", "Skeleton ESP", ModuleCategory.RENDER);
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
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        Player viewer = client.gameRenderer.mainCamera().entity() instanceof Player player ? player : null;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || viewer == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (net.minecraft.world.entity.LivingEntity entity : RenderedEntityCache.entities()) {
            if (!(entity instanceof Player target) || target == viewer || target.isSpectator()) {
                continue;
            }
            if (target.isInvisible() && !this.invisibles.value()) {
                continue;
            }
            if (this.ignoreFriends.value() && AnarchyClient.FRIENDS.isFriend(target.getScoreboardName())) {
                continue;
            }
            double rangeValue = this.range.value();
            if (target.distanceToSqr(viewer) > rangeValue * rangeValue) {
                continue;
            }
            WorldLineRenderer.Color color = AnarchyClient.FRIENDS.isFriend(target.getScoreboardName())
                    ? new WorldLineRenderer.Color(98, 170, 255, this.opacity.value().intValue())
                    : new WorldLineRenderer.Color(245, 245, 245, this.opacity.value().intValue());
            for (Segment segment : skeleton(target, partialTick, camera)) {
                WorldLineRenderer.lineNoDepth(matrices, submits, segment.start(), segment.end(), color);
            }
        }
    }

    static List<Segment> skeleton(final Player player, final float partialTick, final Vec3 camera) {
        AABB box = player.getBoundingBox();
        double height = Math.max(1.2, box.getYsize());
        double shoulderWidth = Math.min(0.45, Math.max(0.25, box.getXsize() * 0.6));
        double hipWidth = shoulderWidth * 0.6;
        Vec3 base = interpolatedPosition(player, partialTick).subtract(camera);
        Vec3 right = rightVector(Mth.lerp(partialTick, player.yRotO, player.getYRot()));

        Vec3 pelvis = base.add(0.0, height * 0.48, 0.0);
        Vec3 chest = base.add(0.0, height * 0.74, 0.0);
        Vec3 neck = base.add(0.0, height * 0.88, 0.0);
        Vec3 head = base.add(0.0, height, 0.0);

        Vec3 leftShoulder = chest.subtract(right.scale(shoulderWidth));
        Vec3 rightShoulder = chest.add(right.scale(shoulderWidth));
        Vec3 leftHand = leftShoulder.subtract(right.scale(shoulderWidth * 0.25)).add(0.0, -height * 0.24, 0.0);
        Vec3 rightHand = rightShoulder.add(right.scale(shoulderWidth * 0.25)).add(0.0, -height * 0.24, 0.0);

        Vec3 leftHip = pelvis.subtract(right.scale(hipWidth));
        Vec3 rightHip = pelvis.add(right.scale(hipWidth));
        Vec3 leftKnee = leftHip.add(0.0, -height * 0.24, 0.0);
        Vec3 rightKnee = rightHip.add(0.0, -height * 0.24, 0.0);
        Vec3 leftFoot = leftKnee.subtract(right.scale(hipWidth * 0.35)).add(0.0, -height * 0.24, 0.0);
        Vec3 rightFoot = rightKnee.add(right.scale(hipWidth * 0.35)).add(0.0, -height * 0.24, 0.0);

        List<Segment> segments = new ArrayList<>();
        segments.add(new Segment(pelvis, chest));
        segments.add(new Segment(chest, neck));
        segments.add(new Segment(neck, head));
        segments.add(new Segment(leftShoulder, rightShoulder));
        segments.add(new Segment(leftShoulder, leftHand));
        segments.add(new Segment(rightShoulder, rightHand));
        segments.add(new Segment(leftHip, rightHip));
        segments.add(new Segment(leftHip, leftKnee));
        segments.add(new Segment(leftKnee, leftFoot));
        segments.add(new Segment(rightHip, rightKnee));
        segments.add(new Segment(rightKnee, rightFoot));
        return List.copyOf(segments);
    }

    private static Vec3 interpolatedPosition(final Player player, final float partialTick) {
        return new Vec3(
                Mth.lerp(partialTick, player.xo, player.getX()),
                Mth.lerp(partialTick, player.yo, player.getY()),
                Mth.lerp(partialTick, player.zo, player.getZ())
        );
    }

    private static Vec3 rightVector(final float yawDegrees) {
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
    }

    record Segment(Vec3 start, Vec3 end) {
    }
}
