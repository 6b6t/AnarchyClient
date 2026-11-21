package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class BuildHeightModule extends Module {

    private final NumberSetting warningDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("warning_distance")
            .name("Warning")
            .defaultValue(5.0)
            .min(1.0)
            .max(32.0)
            .step(1.0)
            .build()));
    private int cooldown;

    public BuildHeightModule() {
        super(
                "build_height",
                "Build Height",
                ModuleCategory.WORLD,
                "Warns you in chat when you get within the set distance of the world's build height limit.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || this.cooldown-- > 0) {
            return;
        }
        int maxY = client.level.getMinY() + client.level.getHeight() - 1;
        int distance = maxY - client.player.blockPosition().getY();
        if (distance <= this.warningDistance.value().intValue()) {
            client.player.sendSystemMessage(Translations.component(
                    "anarchyclient.message.build_height.build_height_in_blocks",
                    "Build height in %s blocks.",
                    Math.max(0, distance)));
            this.cooldown = 60;
        }
    }
}
