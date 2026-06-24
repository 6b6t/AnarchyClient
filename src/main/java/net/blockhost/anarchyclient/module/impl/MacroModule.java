package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class MacroModule extends Module {

    private final StringSetting f6 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f6")
            .name("F6")
            .defaultValue("/home")
            .build()));
    private final StringSetting f7 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f7")
            .name("F7")
            .defaultValue("/spawn")
            .build()));
    private final StringSetting f8 = this.setting(StringSetting.from(StringSetting.builder()
            .id("f8")
            .name("F8")
            .defaultValue("gg")
            .build()));
    private boolean f6Down;
    private boolean f7Down;
    private boolean f8Down;

    public MacroModule() {
        super("macros", "Macros", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        this.f6Down = this.handle(client, GLFW.GLFW_KEY_F6, this.f6.value(), this.f6Down);
        this.f7Down = this.handle(client, GLFW.GLFW_KEY_F7, this.f7.value(), this.f7Down);
        this.f8Down = this.handle(client, GLFW.GLFW_KEY_F8, this.f8.value(), this.f8Down);
    }

    private boolean handle(final Minecraft client, final int key, final String action, final boolean previous) {
        boolean pressed = InputStates.keyPressed(client, key);
        if (pressed && !previous) {
            ChatActions.send(client, action);
        }
        return pressed;
    }
}
