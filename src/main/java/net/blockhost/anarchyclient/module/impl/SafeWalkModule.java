package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;

public final class SafeWalkModule extends Module {

    private final BooleanSetting pauseInGui = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_gui")
            .name("GUI Pause")
            .defaultValue(true)
            .build()));
    private final BooleanSetting blocksOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blocks_only")
            .name("Blocks Only")
            .defaultValue(false)
            .build()));
    private final BooleanSetting sneakOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sneak_only")
            .name("Sneak Only")
            .defaultValue(false)
            .build()));

    public SafeWalkModule() {
        super("safe_walk", "Safe Walk", ModuleCategory.MOVEMENT);
    }

    @Override
    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return client.player == player
                && (!this.pauseInGui.value() || client.gui.screen() == null)
                && (!this.blocksOnly.value() || player.getMainHandItem().getItem() instanceof BlockItem)
                && (!this.sneakOnly.value() || client.options.keyShift.isDown());
    }
}
