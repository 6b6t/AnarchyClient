package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class QuiverModule extends Module {

    private final NumberSetting releaseTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("release_ticks")
            .name("Release")
            .defaultValue(8.0)
            .min(3.0)
            .max(20.0)
            .step(1.0)
            .build()));

    public QuiverModule() {
        super("quiver", "Quiver", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gameMode == null || !client.player.getMainHandItem().is(Items.BOW)) {
            return;
        }
        if (!client.player.isUsingItem()) {
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            return;
        }
        if (client.player.getTicksUsingItem() >= this.releaseTicks.value().intValue()) {
            client.player.stopUsingItem();
        }
    }
}
