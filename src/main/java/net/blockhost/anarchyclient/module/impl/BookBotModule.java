package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class BookBotModule extends Module {

    private final StringListSetting pages = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("pages")
            .name("Pages")
            .addAllDefaultValue(List.of("AnarchyClient page 1", "AnarchyClient page 2"))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(40.0)
            .min(5.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private final BooleanSetting loop = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("loop")
            .name("Loop")
            .defaultValue(false)
            .build()));
    private int pageIndex;
    private int cooldownTicks;

    public BookBotModule() {
        super("book_bot", "Book Bot", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null || this.pages.value().isEmpty()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        ChatActions.send(client, this.pages.value().get(this.pageIndex));
        this.pageIndex++;
        if (this.pageIndex >= this.pages.value().size()) {
            if (!this.loop.value()) {
                this.enabled(false);
                return;
            }
            this.pageIndex = 0;
        }
        this.cooldownTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.pageIndex = 0;
        this.cooldownTicks = 0;
    }
}
