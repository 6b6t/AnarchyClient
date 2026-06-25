package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class InstantRebreakModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private BlockPos lastTarget;
    private int cooldown;

    public InstantRebreakModule() {
        super("instant_rebreak", "Instant Rebreak", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldown > 0) {
            this.cooldown--;
        }
        if (!(client.hitResult instanceof BlockHitResult hit) || client.hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!pos.equals(this.lastTarget)) {
            this.lastTarget = pos.immutable();
            return;
        }
        if (this.cooldown <= 0 && client.level != null && !client.level.getBlockState(pos).isAir()) {
            WorldInteraction.breakBlock(client, pos, Direction.UP, stack -> true);
            this.cooldown = this.delayTicks.value().intValue();
        }
    }
}
