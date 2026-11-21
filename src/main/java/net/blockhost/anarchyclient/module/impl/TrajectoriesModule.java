package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TrajectoriesModule extends Module {

    private static final double AIR_DRAG = 0.99;

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
        super(
                "trajectories",
                "Trajectories",
                ModuleCategory.RENDER,
                "Draws the predicted flight path of the projectile you hold and of thrown items and arrows already in the air.");
    }

    @Override
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        LocalPlayer player = client.player;
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || player == null || matrices == null || submits == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        WorldLineRenderer.Color color =
                new WorldLineRenderer.Color(255, 245, 145, this.opacity.value().intValue());
        // Start where the entities are drawn this frame, not at their last tick position.
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        drawHeldTrajectory(client, player, partialTick, matrices, submits, camera, color);
        WorldLineRenderer.Color flying =
                new WorldLineRenderer.Color(145, 205, 255, this.opacity.value().intValue());
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getDeltaMovement().lengthSqr() <= 0.0025) {
                continue;
            }
            if (entity instanceof ThrowableProjectile) {
                drawSimulated(
                        client,
                        entity.getPosition(partialTick),
                        entity.getDeltaMovement(),
                        entity.getGravity(),
                        true,
                        matrices,
                        submits,
                        camera,
                        flying);
            } else if (entity instanceof AbstractArrow) {
                drawSimulated(
                        client,
                        entity.getPosition(partialTick),
                        entity.getDeltaMovement(),
                        entity.getGravity(),
                        false,
                        matrices,
                        submits,
                        camera,
                        flying);
            }
        }
    }

    private void drawHeldTrajectory(
            final Minecraft client,
            final LocalPlayer player,
            final float partialTick,
            final PoseStack matrices,
            final SubmitNodeCollector submits,
            final Vec3 camera,
            final WorldLineRenderer.Color color) {
        Ballistics ballistics = ballistics(player, player.getMainHandItem());
        if (ballistics == null) {
            ballistics = ballistics(player, player.getOffhandItem());
        }
        if (ballistics == null) {
            return;
        }
        // Vanilla spawns thrown items and arrows 0.1 below the eyes and adds the shooter's movement.
        Vec3 start = player.getEyePosition(partialTick).subtract(0.0, 0.1, 0.0);
        Vec3 shooterMovement = player.getKnownMovement();
        Vec3 motion = launchMotion(player.getXRot(), player.getYRot(), ballistics.pitchOffset(), ballistics.velocity())
                .add(shooterMovement.x, player.onGround() ? 0.0 : shooterMovement.y, shooterMovement.z);
        drawSimulated(
                client,
                start,
                motion,
                ballistics.gravity(),
                ballistics.gravityBeforeMove(),
                matrices,
                submits,
                camera,
                color);
    }

    private void drawSimulated(
            final Minecraft client,
            final Vec3 start,
            final Vec3 initialMotion,
            final double gravity,
            final boolean gravityBeforeMove,
            final PoseStack matrices,
            final SubmitNodeCollector submits,
            final Vec3 camera,
            final WorldLineRenderer.Color color) {
        Vec3 position = start;
        Vec3 motion = initialMotion;
        for (int step = 0; step < this.steps.value().intValue(); step++) {
            if (gravityBeforeMove) {
                motion = throwableStep(motion, gravity);
            }
            Vec3 next = position.add(motion);
            BlockHitResult hit = client.level.clip(
                    new ClipContext(position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, client.player));
            Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
            WorldLineRenderer.lineNoDepth(matrices, submits, position.subtract(camera), end.subtract(camera), color);
            if (hit.getType() != HitResult.Type.MISS) {
                return;
            }
            position = next;
            if (!gravityBeforeMove) {
                motion = arrowStep(motion, gravity);
            }
        }
    }

    /** {@code ThrowableProjectile.tick}: gravity, then air drag, then the move. */
    static Vec3 throwableStep(final Vec3 motion, final double gravity) {
        return motion.add(0.0, -gravity, 0.0).scale(AIR_DRAG);
    }

    /** {@code AbstractArrow.tick}: the move, then air drag, then gravity. */
    static Vec3 arrowStep(final Vec3 motion, final double gravity) {
        return motion.scale(AIR_DRAG).add(0.0, -gravity, 0.0);
    }

    /** {@code Projectile.shootFromRotation} without inaccuracy and shooter movement. */
    static Vec3 launchMotion(final float xRot, final float yRot, final float pitchOffset, final double velocity) {
        double x = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        double y = -Mth.sin((xRot + pitchOffset) * Mth.DEG_TO_RAD);
        double z = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        return new Vec3(x, y, z).normalize().scale(velocity);
    }

    /** Launch values from the vanilla item and projectile classes, or null for items that shoot nothing. */
    static Ballistics ballistics(final LocalPlayer player, final ItemStack stack) {
        if (stack.is(Items.ENDER_PEARL)
                || stack.is(Items.SNOWBALL)
                || stack.is(Items.EGG)
                || stack.is(Items.BLUE_EGG)
                || stack.is(Items.BROWN_EGG)) {
            return new Ballistics(1.5, 0.03, 0.0F, true);
        }
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return new Ballistics(0.5, 0.05, -20.0F, true);
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return new Ballistics(0.7, 0.07, -20.0F, true);
        }
        if (stack.is(Items.TRIDENT)) {
            return new Ballistics(2.5, 0.05, 0.0F, false);
        }
        if (stack.is(Items.BOW)) {
            // While drawing, the arrow speed follows the draw time; otherwise show a full draw.
            float power = player != null && player.isUsingItem() && player.getUseItem() == stack
                    ? BowItem.getPowerForTime(player.getTicksUsingItem())
                    : 1.0F;
            return new Ballistics(power * 3.0, 0.05, 0.0F, false);
        }
        if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
            ChargedProjectiles charged =
                    stack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            return charged.contains(Items.FIREWORK_ROCKET) ? null : new Ballistics(3.15, 0.05, 0.0F, false);
        }
        return null;
    }

    record Ballistics(double velocity, double gravity, float pitchOffset, boolean gravityBeforeMove) {}
}
