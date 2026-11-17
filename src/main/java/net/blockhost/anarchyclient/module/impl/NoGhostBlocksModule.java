package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class NoGhostBlocksModule extends Module {

    private final NumberSetting intervalTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval_ticks")
            .name("Interval")
            .defaultValue(40.0)
            .min(5.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private int timer;

    public NoGhostBlocksModule() {
        super("no_ghost_blocks", "No Ghost Blocks", ModuleCategory.WORLD,
                "Sends an aborted dig for the block you look at so servers that resend it can fix a ghost block. "
                        + "Vanilla servers do not resend blocks for this.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null || this.timer-- > 0) {
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        // One packet for the targeted block only. Sweeping a radius sent up to 15,625 packets per burst, which
        // risks a spam kick and cancels any real mining in progress.
        client.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                hit.getBlockPos(),
                hit.getDirection()
        ));
        this.timer = this.intervalTicks.value().intValue();
    }
}
