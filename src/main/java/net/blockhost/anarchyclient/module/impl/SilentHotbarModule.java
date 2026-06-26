package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public final class SilentHotbarModule extends HudElementModule {

    private final BooleanSetting holdSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("hold_slot")
            .name("Hold Slot")
            .defaultValue(false)
            .build()));
    private final NumberSetting slot = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("slot")
            .name("Slot")
            .defaultValue(1.0)
            .min(1.0)
            .max(9.0)
            .step(1.0)
            .build()));
    private final BooleanSetting restore = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore")
            .name("Restore")
            .defaultValue(true)
            .build()));

    public SilentHotbarModule() {
        super("silent_hotbar", "Silent Hotbar", "Bottom Left");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !this.holdSlot.value()) {
            return;
        }
        SilentHotbar.select(player, this.id(), this.slot.value().intValue() - 1,
                SilentHotbar.PRIORITY_NORMAL, 2, this.restore.value());
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        List<String> lines = new ArrayList<>();
        lines.add("Silent Hotbar");
        if (client.player != null) {
            lines.add("selected " + (client.player.getInventory().getSelectedSlot() + 1));
        }
        if (this.holdSlot.value()) {
            lines.add("holding " + this.slot.value().intValue());
        }
        SilentHotbar.activeOwner().ifPresentOrElse(
                owner -> lines.add(owner + " slot " + displaySlot(SilentHotbar.activeSlot().orElse(-1))),
                () -> lines.add("idle")
        );
        return lines;
    }

    @Override
    protected void onDisable() {
        SilentHotbar.clear(this.id());
    }

    private static int displaySlot(final int zeroBasedSlot) {
        return Inventory.isHotbarSlot(zeroBasedSlot) ? zeroBasedSlot + 1 : -1;
    }
}
