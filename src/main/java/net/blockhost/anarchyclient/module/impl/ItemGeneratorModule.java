package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.command.CreativeItemFactory;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;

public final class ItemGeneratorModule extends Module {

    private final NumberSetting interval = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval")
            .name("Interval")
            .defaultValue(20.0)
            .min(1.0)
            .max(200.0)
            .step(1.0)
            .build()));
    private final BooleanSetting autoDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_disable")
            .name("Auto Disable")
            .defaultValue(false)
            .build()));
    private int ticks;
    private int generated;

    public ItemGeneratorModule() {
        super("item_generator", "Item Generator", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null || !client.player.getAbilities().instabuild) {
            if (this.autoDisable.value()) {
                this.enabled(false);
            }
            return;
        }
        if (++this.ticks < this.interval.value().intValue()) {
            return;
        }
        this.ticks = 0;
        int slot = 36 + client.player.getInventory().getSelectedSlot();
        client.getConnection().send(new ServerboundSetCreativeModeSlotPacket(slot, CreativeItemFactory.randomItem(this.generated++)));
    }

    @Override
    protected void onEnable() {
        this.ticks = 0;
    }
}
