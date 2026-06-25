package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;

import java.util.List;

public final class SwarmModule extends Module {

    private final SelectSetting role = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("role")
            .name("Role")
            .defaultValue("Worker")
            .addAllOptions(List.of("Host", "Worker"))
            .build()));
    private final StringSetting channel = this.setting(StringSetting.from(StringSetting.builder()
            .id("channel")
            .name("Channel")
            .defaultValue("local")
            .build()));

    public SwarmModule() {
        super("swarm", "Swarm", ModuleCategory.MISC);
    }

    String status() {
        return this.role.value() + ":" + this.channel.value();
    }
}
