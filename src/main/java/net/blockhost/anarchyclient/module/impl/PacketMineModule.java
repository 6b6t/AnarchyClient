package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class PacketMineModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting swing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("swing")
            .name("Swing")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;
    private BlockPos lastTarget;

    public PacketMineModule() {
        super("packet_mine", "Packet Mine", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.getConnection() == null || client.hitResult == null) {
            return;
        }
        if (this.cooldownTicks-- > 0 || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockPos pos = hit.getBlockPos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > this.range.value() * this.range.value()) {
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        Direction direction = hit.getDirection();
        if (this.rotate.value()) {
            RotationManager.request(new RotationRequest(
                    this.id(),
                    Rotation.lookingAt(hit.getLocation(), player.getEyePosition()),
                    70,
                    60.0F,
                    2,
                    2.0F,
                    RotationTurnMode.STEPPED,
                    true
            ));
            RotationManager.apply(player);
        }
        client.getConnection().getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                pos,
                direction
        ));
        client.getConnection().getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                pos,
                direction
        ));
        if (this.swing.value()) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        this.lastTarget = pos;
        this.cooldownTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
        this.lastTarget = null;
        RotationManager.clear(this.id());
    }
}
