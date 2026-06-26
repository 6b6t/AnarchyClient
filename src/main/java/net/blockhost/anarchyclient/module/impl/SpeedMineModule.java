package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
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

import java.util.List;

public final class SpeedMineModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Packet")
            .addAllOptions(List.of("Packet", "Legit"))
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(1.8)
            .min(1.0)
            .max(8.0)
            .step(0.1)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(1.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting requireAttack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_attack")
            .name("Mining Only")
            .defaultValue(true)
            .build()));
    private final BooleanSetting autoTool = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_tool")
            .name("Auto Tool")
            .defaultValue(true)
            .build()));
    private MiningTarget target;
    private int cooldownTicks;

    public SpeedMineModule() {
        super("speed_mine", "Speed Mine", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.getConnection() == null || client.hitResult == null) {
            this.reset(client);
            return;
        }
        if (this.requireAttack.value() && !client.options.keyAttack.isDown()) {
            this.reset(client);
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (client.hitResult.getType() != HitResult.Type.BLOCK) {
            this.reset(client);
            return;
        }
        BlockHitResult hit = (BlockHitResult) client.hitResult;
        BlockPos pos = hit.getBlockPos();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > this.range.value() * this.range.value()) {
            this.reset(client);
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            this.reset(client);
            return;
        }
        if (this.autoTool.value()) {
            int slot = AutoToolModule.bestToolSlot(player.getInventory(), state, true);
            if (slot >= 0) {
                InventoryActions.selectHotbarSlot(player, slot);
            }
        }
        if ("Legit".equals(this.mode.value())) {
            WorldInteraction.breakBlock(client, pos, hit.getDirection(), stack -> true);
            return;
        }
        if (this.target == null || !this.target.matches(pos, hit.getDirection())) {
            this.reset(client);
            this.target = new MiningTarget(pos.immutable(), hit.getDirection(), 0.0F);
            client.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    pos,
                    hit.getDirection()
            ));
        }
        float progress = this.target.progress()
                + state.getDestroyProgress(player, client.level, pos) * this.speed.value().floatValue();
        this.target = new MiningTarget(pos.immutable(), hit.getDirection(), progress);
        this.debugValue("progress", Math.min(100, Math.round(progress * 100.0F)) + "%");
        if (progress >= 1.0F) {
            client.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    pos,
                    hit.getDirection()
            ));
            player.swing(InteractionHand.MAIN_HAND);
            this.target = null;
            this.cooldownTicks = this.cooldown.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        this.reset(Minecraft.getInstance());
        this.cooldownTicks = 0;
    }

    private void reset(final Minecraft client) {
        if (this.target != null && client.getConnection() != null) {
            client.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    this.target.pos(),
                    this.target.face()
            ));
        }
        this.target = null;
    }

    private record MiningTarget(BlockPos pos, Direction face, float progress) {

        boolean matches(final BlockPos pos, final Direction face) {
            return this.pos.equals(pos) && this.face == face;
        }
    }
}
