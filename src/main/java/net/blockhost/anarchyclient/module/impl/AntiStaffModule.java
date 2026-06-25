package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class AntiStaffModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(80.0)
            .min(8.0)
            .max(256.0)
            .step(8.0)
            .build()));
    private final Set<String> alerted = new LinkedHashSet<>();

    public AntiStaffModule() {
        super("anti_staff", "Anti Staff", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.alerted.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.range.value() * this.range.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player
                    && player != client.player
                    && client.player.distanceToSqr(player) <= rangeSqr
                    && TargetClassifier.looksLikeStaffName(player.getScoreboardName())
                    && this.alerted.add(player.getScoreboardName().toLowerCase(Locale.ROOT))) {
                client.player.sendSystemMessage(Component.literal("Possible staff nearby: " + player.getScoreboardName() + "."));
            }
        }
    }
}
