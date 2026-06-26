package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class VelocityModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Scale")
            .addAllOptions(List.of("Scale", "Cancel", "Reverse", "Jump Reset"))
            .build()));
    private final NumberSetting horizontal = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("horizontal")
            .name("Horizontal")
            .defaultValue(0.0)
            .min(0.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.0)
            .min(0.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final BooleanSetting allEntities = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("all_entities")
            .name("All Entities")
            .defaultValue(false)
            .build()));
    private final NumberSetting chance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("chance")
            .name("Chance")
            .defaultValue(100.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final BooleanSetting onlyGround = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_ground")
            .name("Ground Only")
            .defaultValue(false)
            .build()));
    private int jumpResetTicks;

    public VelocityModule() {
        super("velocity", "Velocity", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.jumpResetTicks <= 0 || client.player == null) {
            return;
        }
        this.jumpResetTicks--;
        if (client.player.onGround()) {
            Vec3 velocity = client.player.getDeltaMovement();
            client.player.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.42), velocity.z);
            this.jumpResetTicks = 0;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket motion)
                || client.player == null
                || client.level == null) {
            return false;
        }
        Entity entity = client.level.getEntity(motion.id());
        if (entity == null || !this.allEntities.value() && entity != client.player) {
            return false;
        }
        if (this.onlyGround.value() && entity == client.player && !client.player.onGround()) {
            return false;
        }
        if (ThreadLocalRandom.current().nextDouble(100.0) >= this.chance.value()) {
            return false;
        }
        switch (this.mode.value()) {
            case "Cancel" -> {
                return true;
            }
            case "Reverse" -> entity.setDeltaMovement(scaleMotion(motion.movement().reverse(),
                    this.horizontal.value(), this.vertical.value()));
            case "Jump Reset" -> {
                if (entity == client.player) {
                    this.jumpResetTicks = 3;
                }
                entity.setDeltaMovement(scaleMotion(motion.movement(), this.horizontal.value(), this.vertical.value()));
            }
            default -> entity.setDeltaMovement(scaleMotion(motion.movement(), this.horizontal.value(), this.vertical.value()));
        }
        return true;
    }

    static Vec3 scaleMotion(final Vec3 movement, final double horizontalPercent, final double verticalPercent) {
        double horizontalScale = horizontalPercent / 100.0;
        double verticalScale = verticalPercent / 100.0;
        return new Vec3(movement.x * horizontalScale, movement.y * verticalScale, movement.z * horizontalScale);
    }
}
