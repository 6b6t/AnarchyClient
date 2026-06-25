package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoQueueModule extends Module {

    private final StringSetting trigger = this.setting(StringSetting.from(StringSetting.builder()
            .id("trigger")
            .name("Trigger")
            .defaultValue("You died")
            .build()));
    private final StringSetting action = this.setting(StringSetting.from(StringSetting.builder()
            .id("action")
            .name("Action")
            .defaultValue("/play")
            .build()));
    private final BooleanSetting matchCase = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("match_case")
            .name("Case")
            .defaultValue(false)
            .build()));
    private final NumberSetting cooldown = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown")
            .name("Cooldown")
            .defaultValue(60.0)
            .min(0.0)
            .max(1200.0)
            .step(5.0)
            .build()));
    private int cooldownTicks;

    public AutoQueueModule() {
        super("auto_queue", "Auto Queue", ModuleCategory.PLAYER, java.util.List.of("auto_play"));
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (client.player != null && this.cooldownTicks <= 0 && matches(message.getString(), this.trigger.value(), this.matchCase.value())) {
            ChatActions.send(client, this.action.value());
            this.cooldownTicks = this.cooldown.value().intValue();
        }
        return message;
    }

    static boolean matches(final String message, final String trigger, final boolean matchCase) {
        if (message == null || trigger == null || trigger.isBlank()) {
            return false;
        }
        if (matchCase) {
            return message.contains(trigger);
        }
        return message.toLowerCase(java.util.Locale.ROOT).contains(trigger.toLowerCase(java.util.Locale.ROOT));
    }
}
