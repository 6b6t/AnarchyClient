package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class PopChamsModule extends Module {

    private static final byte TOTEM_POP_EVENT = 35;

    private final NumberSetting lifetimeTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime_ticks")
            .name("Lifetime")
            .defaultValue(30.0)
            .min(5.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final List<PopGhost> ghosts = new ArrayList<>();

    public PopChamsModule() {
        super("pop_chams", "Pop Chams", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        Iterator<PopGhost> iterator = this.ghosts.iterator();
        while (iterator.hasNext()) {
            PopGhost ghost = iterator.next();
            ghost.age++;
            if (ghost.age > this.lifetimeTicks.value().intValue()) {
                iterator.remove();
            }
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.level != null
                && packet instanceof ClientboundEntityEventPacket event
                && event.getEventId() == TOTEM_POP_EVENT) {
            Entity entity = event.getEntity(client.level);
            if (entity instanceof LivingEntity living) {
                this.ghosts.add(new PopGhost(living.getBoundingBox(), 0));
            }
        }
        return false;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null || this.ghosts.isEmpty()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        int lifetime = Math.max(1, this.lifetimeTicks.value().intValue());
        for (PopGhost ghost : this.ghosts) {
            int alpha = Math.max(20, 180 - ghost.age * 180 / lifetime);
            AABB box = ghost.box.move(0.0, ghost.age * 0.02, 0.0).move(camera.scale(-1));
            WorldLineRenderer.fillNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 210, 90, Math.max(12, alpha / 4)));
            WorldLineRenderer.boxNoDepth(matrices, submits, box, new WorldLineRenderer.Color(255, 210, 90, alpha));
        }
    }

    private static final class PopGhost {
        private final AABB box;
        private int age;

        private PopGhost(final AABB box, final int age) {
            this.box = box;
            this.age = age;
        }
    }
}
