package net.blockhost.anarchyclient.module.impl;

import java.util.List;
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
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class SpeedMineModule extends Module {

    /** The server finishes an early stop only when its own progress count reaches this value. */
    static final float SERVER_BREAK_THRESHOLD = 0.7F;

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Packet")
            .addAllOptions(List.of("Packet", "Legit"))
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(1.4)
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
        super(
                "speed_mine",
                "Speed Mine",
                ModuleCategory.WORLD,
                "Mines blocks faster by sending the finish-digging packet early, or by repeated legit breaking. "
                        + "Packet mode never stops before 70% progress, the earliest point the server accepts.");
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
        if (!CombatTargets.canReachBlock(player, pos, Math.min(this.range.value(), player.blockInteractionRange()))) {
            this.reset(client);
            return;
        }
        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) {
            this.reset(client);
            return;
        }
        if (this.autoTool.value()) {
            int slot = AutoToolModule.bestToolSlot(player.getInventory(), state);
            if (slot >= 0) {
                InventoryActions.selectHotbarSlot(player, slot);
            }
        }
        if ("Legit".equals(this.mode.value())) {
            WorldInteraction.breakBlockWithHeldItem(client, this, pos, hit.getDirection());
            return;
        }
        if (this.target == null || !this.target.matches(pos, hit.getDirection())) {
            this.reset(client);
            this.target = new MiningTarget(pos.immutable(), hit.getDirection(), 0.0F);
            client.getConnection()
                    .send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, hit.getDirection()));
        }
        float progress = this.target.progress() + state.getDestroyProgress(player, client.level, pos);
        this.target = new MiningTarget(pos.immutable(), hit.getDirection(), progress);
        this.debugValue("progress", Math.min(100, Math.round(progress * 100.0F)) + "%");
        if (readyToStop(progress, this.speed.value().floatValue())) {
            client.getConnection()
                    .send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, hit.getDirection()));
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            this.target = null;
            this.cooldownTicks = this.cooldown.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        this.reset(Minecraft.getInstance());
        this.cooldownTicks = 0;
    }

    /**
     * Returns whether the real mining progress, counted from the start packet, is enough to send
     * the stop packet. The speed multiplier shortens the wait, but never below the server's
     * threshold, where it would reject the stop and keep the block.
     */
    static boolean readyToStop(final float realProgress, final float speed) {
        return realProgress >= SERVER_BREAK_THRESHOLD && realProgress * speed >= 1.0F;
    }

    private void reset(final Minecraft client) {
        WorldInteraction.stopBreaking(client, this);
        if (this.target != null && client.getConnection() != null) {
            client.getConnection()
                    .send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                            this.target.pos(),
                            this.target.face()));
        }
        this.target = null;
    }

    private record MiningTarget(BlockPos pos, Direction face, float progress) {

        boolean matches(final BlockPos pos, final Direction face) {
            return this.pos.equals(pos) && this.face == face;
        }
    }
}
