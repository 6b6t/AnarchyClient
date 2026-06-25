package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;

public final class FlamethrowerModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(7.0)
            .step(1.0)
            .build()));
    private final NumberSetting actions = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("actions")
            .name("Actions")
            .defaultValue(1.0)
            .min(1.0)
            .max(4.0)
            .step(1.0)
            .build()));

    public FlamethrowerModule() {
        super("flamethrower", "Flamethrower", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int used = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.range.value().intValue(),
                this.range.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                32,
                candidate -> !candidate.state().isAir() && client.level.isEmptyBlock(candidate.pos().above())
        )) {
            if (WorldInteraction.useOnBlock(client, this, target.pos(), Direction.UP,
                    stack -> stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE), true)
                    == WorldInteraction.ActionResult.DONE && ++used >= this.actions.value().intValue()) {
                return;
            }
        }
    }
}
