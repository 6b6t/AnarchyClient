package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class DiscordPresenceModule extends Module {

    private final StringSetting status = this.setting(StringSetting.from(StringSetting.builder()
            .id("status")
            .name("Status")
            .defaultValue("Playing AnarchyClient")
            .build()));

    public DiscordPresenceModule() {
        super("discord_presence", "Discord Presence", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        // Stores the configured rich-presence text without pulling in a Discord RPC dependency.
    }

    String status() {
        return this.status.value();
    }
}
