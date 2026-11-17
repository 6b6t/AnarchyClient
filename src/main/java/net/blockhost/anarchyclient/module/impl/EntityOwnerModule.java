package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.WorldLineRenderer;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public final class EntityOwnerModule extends Module {

    private static final double LABEL_RANGE = 64.0;

    public EntityOwnerModule() {
        super("entity_owner", "Entity Owner", ModuleCategory.RENDER,
                "Outlines every tamed animal that has an owner through walls and labels nearby ones with the owner's name.");
    }

    @Override
    public void renderWorld(final Minecraft client, final LevelRenderContext context) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.level == null || matrices == null || submits == null) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof TamableAnimal animal && animal.getOwnerReference() != null) {
                WorldLineRenderer.boxNoDepth(matrices, submits, entity.getBoundingBox().inflate(0.05).move(camera.scale(-1)),
                        new WorldLineRenderer.Color(120, 230, 255, 210));
            }
        }
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.player == null || client.level == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof TamableAnimal animal
                    && animal.getOwnerReference() != null
                    && animal.distanceToSqr(client.player) <= LABEL_RANGE * LABEL_RANGE) {
                this.renderLabel(client, graphics, animal, animal.getOwnerReference(), partialTick);
            }
        }
    }

    private void renderLabel(final Minecraft client, final GuiGraphicsExtractor graphics, final TamableAnimal animal,
                             final EntityReference<LivingEntity> owner, final float partialTick) {
        double x = Mth.lerp(partialTick, animal.xo, animal.getX());
        double y = Mth.lerp(partialTick, animal.yo, animal.getY()) + animal.getBoundingBox().getYsize() + 0.5;
        double z = Mth.lerp(partialTick, animal.zo, animal.getZ());
        Vec3 projected = client.gameRenderer.projectPointToScreen(new Vec3(x, y, z));
        if (projected.z > 1.0) {
            return;
        }
        String text = ownerLabel(ownerName(client.getConnection(), owner.getUUID()), owner.getUUID());
        int screenX = (int) ((projected.x + 1.0) * 0.5 * graphics.guiWidth());
        int screenY = (int) ((1.0 - projected.y) * 0.5 * graphics.guiHeight());
        int halfWidth = client.font.width(text) / 2;
        graphics.fill(screenX - halfWidth - 2, screenY - 2, screenX + halfWidth + 2, screenY + 9, 0x70000000);
        graphics.text(client.font, text, screenX - halfWidth, screenY, 0xFF78E6FF, true);
    }

    static String ownerLabel(final @Nullable String ownerName, final UUID ownerId) {
        String owner = ownerName == null || ownerName.isBlank() ? ownerId.toString().substring(0, 8) : ownerName;
        return "Owner: " + owner;
    }

    private static @Nullable String ownerName(final @Nullable ClientPacketListener connection, final UUID ownerId) {
        if (connection == null) {
            return null;
        }
        PlayerInfo info = connection.getPlayerInfo(ownerId);
        return info == null ? null : info.getProfile().name();
    }
}
