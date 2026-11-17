package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

public final class InstantRebreakModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private @Nullable BlockPos attacked;
    private Direction attackedFace = Direction.UP;
    private @Nullable BlockPos rebreakPos;
    private Direction rebreakFace = Direction.UP;
    private int cooldown;

    public InstantRebreakModule() {
        super("instant_rebreak", "Instant Rebreak", ModuleCategory.WORLD,
                "After you mine a block, breaks it again instantly each time a block reappears at that spot. "
                        + "Uses the item in your hand.");
    }

    @Override
    public boolean attackBlock(final Minecraft client, final BlockPos pos, final Direction direction) {
        if (!pos.equals(this.rebreakPos)) {
            // Starting a new break moves the server's break position, so the old spot no longer rebreaks.
            this.rebreakPos = null;
        }
        this.attacked = pos.immutable();
        this.attackedFace = direction;
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (client.player == null || client.level == null || client.getConnection() == null) {
            return;
        }
        if (this.attacked != null && client.level.getBlockState(this.attacked).isAir()) {
            this.rebreakPos = this.attacked;
            this.rebreakFace = this.attackedFace;
            this.attacked = null;
        }
        BlockPos pos = this.rebreakPos;
        if (pos == null || this.cooldown > 0 || client.level.getBlockState(pos).isAir()
                || !CombatTargets.canReachBlock(client.player, pos)) {
            return;
        }
        // The server still remembers this spot as the block being mined, so a stop packet breaks it at once.
        client.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, this.rebreakFace));
        client.player.swing(InteractionHand.MAIN_HAND, client.player.getMainHandItem().getAttackAnimation(), false);
        client.player.connection.send(ServerboundPunchPacket.INSTANCE);
        this.cooldown = this.delayTicks.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.attacked = null;
        this.rebreakPos = null;
        this.cooldown = 0;
    }
}
