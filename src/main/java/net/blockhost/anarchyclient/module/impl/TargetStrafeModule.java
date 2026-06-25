package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class TargetStrafeModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.25)
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.28)
            .min(0.05)
            .max(1.0)
            .step(0.01)
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(2.6)
            .min(1.0)
            .max(6.0)
            .step(0.1)
            .build()));
    private final BooleanSetting jump = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("jump")
            .name("Jump")
            .defaultValue(true)
            .build()));
    private int direction = 1;

    public TargetStrafeModule() {
        super("target_strafe", "Target Strafe", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (player == null || target == null || !MovementVelocity.moving(client)) {
            return;
        }
        if (player.horizontalCollision) {
            this.direction *= -1;
        }
        Vec3 away = player.position().subtract(target.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        if (flat.horizontalDistanceSqr() < 1.0E-6) {
            return;
        }
        Vec3 radial = flat.normalize();
        Vec3 tangent = new Vec3(-radial.z * this.direction, 0.0, radial.x * this.direction);
        double distance = Math.sqrt(flat.horizontalDistanceSqr());
        double correction = Math.max(-0.45, Math.min(0.45, (this.radius.value() - distance) * 0.35));
        Vec3 velocity = tangent.add(radial.scale(correction)).normalize().scale(this.speed.value());
        player.setDeltaMovement(velocity.x, player.getDeltaMovement().y, velocity.z);
        if (this.jump.value() && player.onGround()) {
            player.jumpFromGround();
        }
        this.debugValue("target", target.getScoreboardName());
    }

    @Override
    protected void onDisable() {
        this.clearDebugValues();
    }
}
