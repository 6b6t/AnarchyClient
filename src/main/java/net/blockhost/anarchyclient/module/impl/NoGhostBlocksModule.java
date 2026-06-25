package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public final class NoGhostBlocksModule extends Module {

    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(4.0)
            .min(1.0)
            .max(12.0)
            .step(1.0)
            .build()));
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
        super("no_ghost_blocks", "No Ghost Blocks", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null || this.timer-- > 0) {
            return;
        }
        BlockPos center = client.player.blockPosition();
        int range = this.radius.value().intValue();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    client.getConnection().send(new ServerboundPlayerActionPacket(
                            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                            center.offset(x, y, z),
                            Direction.UP
                    ));
                }
            }
        }
        this.timer = this.intervalTicks.value().intValue();
    }
}
