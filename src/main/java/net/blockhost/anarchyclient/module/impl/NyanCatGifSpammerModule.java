package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

public final class NyanCatGifSpammerModule extends Module {

    private final NumberSetting cooldownSeconds = this.setting(new NumberSetting("cooldown_seconds", "Cooldown", 8.0, 2.0, 60.0, 1.0));
    private final StringSetting message = this.setting(new StringSetting("message", "Message", "[Nyan] rainbow trail online"));
    private long nextSendMillis;

    public NyanCatGifSpammerModule() {
        super("nyan_cat_gif_spammer", "Nyan Cat GIF Spammer", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < this.nextSendMillis) {
            return;
        }
        String text = this.message.value().trim();
        if (!text.isEmpty()) {
            client.getConnection().sendChat(text);
        }
        this.nextSendMillis = now + Math.round(this.cooldownSeconds.value() * 1000.0);
    }
}
