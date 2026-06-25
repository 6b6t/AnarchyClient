package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ExtraElytraModule extends Module {

    private final BooleanSetting instantFly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("instant_fly")
            .name("Instant Fly")
            .defaultValue(true)
            .build()));
    private final BooleanSetting speedControl = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("speed_control")
            .name("Speed Ctrl")
            .defaultValue(true)
            .build()));
    private final BooleanSetting heightControl = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("height_control")
            .name("Height Ctrl")
            .defaultValue(false)
            .build()));
    private final BooleanSetting stopInWater = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("stop_in_water")
            .name("Water Stop")
            .defaultValue(true)
            .build()));
    private int jumpTimer;

    public ExtraElytraModule() {
        super("extra_elytra", "Extra Elytra", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.jumpTimer = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null || client.gui.screen() != null) {
            return;
        }
        if (this.jumpTimer > 0) {
            this.jumpTimer--;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!canGlide(chest)) {
            return;
        }
        if (player.isFallFlying()) {
            if (this.stopInWater.value() && player.isInWater()) {
                sendFallFlyingPacket(player);
                return;
            }
            this.controlSpeed(client, player);
            this.controlHeight(client, player);
            return;
        }
        if (this.instantFly.value() && client.options.keyJump.isDown()) {
            this.startFallFlying(player);
        }
    }

    private void controlSpeed(final Minecraft client, final LocalPlayer player) {
        if (!this.speedControl.value()) {
            return;
        }
        Vec3 forward = horizontalForward(player.getYRot()).scale(0.05);
        Vec3 velocity = player.getDeltaMovement();
        if (client.options.keyUp.isDown()) {
            player.setDeltaMovement(velocity.add(forward));
        } else if (client.options.keyDown.isDown()) {
            player.setDeltaMovement(velocity.subtract(forward));
        }
    }

    private void controlHeight(final Minecraft client, final LocalPlayer player) {
        if (!this.heightControl.value()) {
            return;
        }
        Vec3 velocity = player.getDeltaMovement();
        if (client.options.keyJump.isDown()) {
            player.setDeltaMovement(velocity.x, velocity.y + 0.08, velocity.z);
        } else if (client.options.keyShift.isDown()) {
            player.setDeltaMovement(velocity.x, velocity.y - 0.04, velocity.z);
        }
    }

    private void startFallFlying(final LocalPlayer player) {
        if (this.jumpTimer <= 0) {
            this.jumpTimer = 20;
            player.setJumping(false);
            player.setSprinting(true);
            player.jumpFromGround();
        }
        sendFallFlyingPacket(player);
    }

    private static void sendFallFlyingPacket(final LocalPlayer player) {
        player.connection.send(new ServerboundPlayerCommandPacket(
                player,
                ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));
    }

    static boolean canGlide(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.has(DataComponents.GLIDER)
                && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    static Vec3 horizontalForward(final float yawDegrees) {
        float yaw = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }
}
