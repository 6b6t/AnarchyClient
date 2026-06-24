package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TrajectoriesModule extends Module {

    private final NumberSetting steps = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("steps")
            .name("Steps")
            .defaultValue(80.0)
            .min(20.0)
            .max(160.0)
            .step(10.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(190.0)
            .min(40.0)
            .max(255.0)
            .step(5.0)
            .build()));

    public TrajectoriesModule() {
        super("trajectories", "Trajectories", ModuleCategory.RENDER);
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || player == null || matrices == null || submits == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color = new WorldLineRenderer.Color(255, 245, 145, this.opacity.value().intValue());
        drawHeldTrajectory(client, player, matrices, submits, camera, color);
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Projectile projectile && projectile.getDeltaMovement().lengthSqr() > 0.0025) {
                drawSimulated(client, projectile.position(), projectile.getDeltaMovement(), 0.03, matrices, submits, camera,
                        new WorldLineRenderer.Color(145, 205, 255, this.opacity.value().intValue()));
            }
        }
    }

    private void drawHeldTrajectory(final Minecraft client, final LocalPlayer player, final PoseStack matrices,
                                    final SubmitNodeCollector submits, final Vec3 camera, final WorldLineRenderer.Color color) {
        ItemStack stack = player.getMainHandItem();
        double velocity = initialVelocity(stack.getItem());
        if (velocity <= 0.0) {
            stack = player.getOffhandItem();
            velocity = initialVelocity(stack.getItem());
        }
        if (velocity <= 0.0) {
            return;
        }
        Vec3 start = player.getEyePosition().add(player.getViewVector(0.0F).scale(0.2));
        Vec3 motion = player.getViewVector(0.0F).normalize().scale(velocity);
        drawSimulated(client, start, motion, gravity(stack.getItem()), matrices, submits, camera, color);
    }

    private void drawSimulated(final Minecraft client, final Vec3 start, final Vec3 initialMotion, final double gravity,
                               final PoseStack matrices, final SubmitNodeCollector submits, final Vec3 camera,
                               final WorldLineRenderer.Color color) {
        Vec3 position = start;
        Vec3 motion = initialMotion;
        for (int step = 0; step < this.steps.value().intValue(); step++) {
            Vec3 next = position.add(motion);
            BlockHitResult hit = client.level.clip(new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
            WorldLineRenderer.line(matrices, submits, position.subtract(camera), end.subtract(camera), color);
            if (hit.getType() != HitResult.Type.MISS) {
                return;
            }
            position = next;
            motion = motion.scale(0.99).add(0.0, -gravity, 0.0);
        }
    }

    private static double initialVelocity(final Item item) {
        if (item == Items.ENDER_PEARL || item == Items.SNOWBALL || item == Items.EGG || item == Items.EXPERIENCE_BOTTLE) {
            return 1.5;
        }
        if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return 0.9;
        }
        if (item == Items.TRIDENT) {
            return 2.5;
        }
        if (item == Items.BOW || item == Items.CROSSBOW) {
            return 3.0;
        }
        return 0.0;
    }

    private static double gravity(final Item item) {
        if (item == Items.BOW || item == Items.CROSSBOW || item == Items.TRIDENT) {
            return 0.05;
        }
        return 0.03;
    }
}
