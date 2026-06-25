package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

public final class AutoEXPModule extends Module {

    private final NumberSetting minArmorPercent = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_armor_percent")
            .name("Armor %")
            .defaultValue(85.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(3.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoEXPModule() {
        super("auto_exp", "Auto EXP", ModuleCategory.COMBAT, java.util.List.of("exp_thrower"));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (!EXPThrowerModule.needsArmorRepair(player, this.minArmorPercent.value())) {
            return;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.EXPERIENCE_BOTTLE));
        if (slot.isEmpty()) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_EQUIPMENT,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.useHotbarItem(slot.orElseThrow(), Items.EXPERIENCE_BOTTLE, true)
        ));
        this.cooldownTicks = this.delay.value().intValue();
    }
}
