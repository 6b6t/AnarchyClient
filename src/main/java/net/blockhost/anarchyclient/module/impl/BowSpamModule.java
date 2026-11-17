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
        super("bow_spam", "Bow Spam", ModuleCategory.COMBAT,
                "Releases a drawn bow after a set number of charge ticks so you fire arrows rapidly.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player != null
                && client.gameMode != null
                && client.player.isUsingItem()
                && client.player.getUseItem().is(Items.BOW)
                && client.player.getTicksUsingItem() >= this.chargeTicks.value().intValue()) {
            // stopUsingItem only stops the client; releaseUsingItem tells the server to fire.
            client.gameMode.releaseUsingItem(client.player);
        }
    }
}
