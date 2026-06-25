package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GhostModeModule extends Module {

    private final BooleanSetting respawnOnDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("respawn_on_disable")
            .name("Respawn")
            .defaultValue(true)
            .build()));
    private boolean ghosted;

    public GhostModeModule() {
        super("ghost_mode", "Ghost Mode", ModuleCategory.PLAYER);
    }

    @Override
    public boolean openScreen(final Minecraft client, final Screen screen) {
        if (screen == null && client.player != null && client.player.isDeadOrDying()) {
            this.ghosted = true;
            client.player.sendSystemMessage(Component.literal("Ghost Mode held the death screen. Disable it to respawn."));
            return true;
        }
        return false;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || !this.ghosted) {
            return;
        }
        client.player.setHealth(Math.max(client.player.getHealth(), 1.0F));
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.ghosted && this.respawnOnDisable.value() && client.player != null && client.player.isDeadOrDying()) {
            client.player.respawn();
        }
        this.ghosted = false;
    }
}
