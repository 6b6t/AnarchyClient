package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;

public final class MultiActionsModule extends Module {

    private static MultiActionsModule instance;
    private final BooleanSetting attackWhileUsing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("attack_while_using")
            .name("Attack")
            .defaultValue(true)
            .build()));
    private final BooleanSetting useWhileBusy = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_while_busy")
            .name("Use")
            .defaultValue(true)
            .build()));

    public MultiActionsModule() {
        super("multi_actions", "Multi Actions", ModuleCategory.MISC);
        instance = this;
    }

    public static boolean attackWhileUsing() {
        return instance != null && instance.enabled() && instance.attackWhileUsing.value();
    }

    public static boolean useWhileBusy() {
        return instance != null && instance.enabled() && instance.useWhileBusy.value();
    }
}
