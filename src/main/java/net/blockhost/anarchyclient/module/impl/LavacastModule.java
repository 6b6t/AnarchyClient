package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class LavacastModule extends Module {

    private final NumberSetting interval = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval")
            .name("Interval")
            .defaultValue(12.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(false)
            .build()));
    private int ticks;
    private boolean lava = true;

    public LavacastModule() {
        super("lavacast", "Lavacast", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (++this.ticks < this.interval.value().intValue()) {
            return;
        }
        this.ticks = 0;
        WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                client,
                this,
                hit.getBlockPos(),
                hit.getDirection(),
                stack -> stack.getItem() == (this.lava ? Items.LAVA_BUCKET : Items.WATER_BUCKET),
                this.rotate.value()
        );
        if (result == WorldInteraction.ActionResult.DONE) {
            this.lava = !this.lava;
        }
    }
}
