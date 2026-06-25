package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class BowSpamModule extends Module {

    private final NumberSetting chargeTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("charge_ticks")
            .name("Charge")
            .defaultValue(3.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));

    public BowSpamModule() {
        super("bow_spam", "Bow Spam", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null
                && client.player.isUsingItem()
                && client.player.getUseItem().is(Items.BOW)
                && client.player.getTicksUsingItem() >= this.chargeTicks.value().intValue()) {
            client.player.stopUsingItem();
        }
    }
}
