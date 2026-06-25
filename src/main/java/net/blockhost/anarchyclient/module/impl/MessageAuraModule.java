package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageAuraModule extends Module {

    private final StringSetting message = this.setting(StringSetting.from(StringSetting.builder()
            .id("message")
            .name("Message")
            .defaultValue("Hello {player}")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(12.0)
            .min(2.0)
            .max(128.0)
            .step(1.0)
            .build()));
    private final NumberSetting cooldownSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown_seconds")
            .name("Cooldown")
            .defaultValue(30.0)
            .min(1.0)
            .max(600.0)
            .step(1.0)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Ignore Friends")
            .defaultValue(true)
            .build()));
    private final Map<UUID, Integer> cooldowns = new HashMap<>();

    public MessageAuraModule() {
        super("message_aura", "Message Aura", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        this.cooldowns.replaceAll((ignored, value) -> Math.max(0, value - 1));
        if (client.player == null || client.level == null || client.getConnection() == null || client.gui.screen() != null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Player player : client.level.players()) {
            if (player == client.player || player.distanceToSqr(client.player) > rangeSqr
                    || this.ignoreFriends.value() && AnarchyClient.FRIENDS.isFriend(player.getName().getString())
                    || this.cooldowns.getOrDefault(player.getUUID(), 0) > 0) {
                continue;
            }
            ChatActions.send(client, this.message.value().replace("{player}", player.getName().getString()));
            this.cooldowns.put(player.getUUID(), Math.max(20, this.cooldownSeconds.value().intValue() * 20));
            return;
        }
    }
}
