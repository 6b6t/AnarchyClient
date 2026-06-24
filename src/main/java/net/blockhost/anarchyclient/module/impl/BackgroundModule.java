package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.setting.SelectSetting;

public final class BackgroundModule extends Module {

    private final SelectSetting design = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("design")
            .name("Background Design")
            .defaultValue(BackgroundDesign.NONE.displayName())
            .addAllOptions(BackgroundDesign.displayNames())
            .build()));

    public BackgroundModule() {
        super("background", "Background", ModuleCategory.MISC);
        this.enabled(true);
    }

    /**
     * Resolves the backdrop drawn behind the client menu panels. Returns {@link BackgroundDesign#NONE}
     * while the module is disabled so the toggle doubles as a master switch.
     */
    public BackgroundDesign selectedDesign() {
        if (!this.enabled()) {
            return BackgroundDesign.NONE;
        }
        return BackgroundDesign.fromDisplayName(this.design.value());
    }
}
