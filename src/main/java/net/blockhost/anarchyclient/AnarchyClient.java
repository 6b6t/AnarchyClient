package net.blockhost.anarchyclient;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.impl.AutoTotemModule;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import org.lwjgl.glfw.GLFW;

public final class AnarchyClient implements ClientModInitializer {

    public static final String MOD_ID = "anarchyclient";
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ClientConfig CONFIG = new ClientConfig(MODULES);

    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        MODULES.register(new AutoTotemModule());
        MODULES.register(new NyanCatGifSpammerModule());
        CONFIG.load();

        this.openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.anarchyclient.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(final Minecraft client) {
        while (this.openMenuKey.consumeClick()) {
            client.setScreen(new AnarchyClientScreen(MODULES, CONFIG));
        }
        MODULES.tick(client);
    }
}
