package net.blockhost.anarchyclient;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnarchyClient implements ClientModInitializer {

    public static final String MOD_ID = "anarchyclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModuleManager MODULES = new ModuleManager();
    public static final ClientConfig CONFIG = new ClientConfig(MODULES);

    private KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        ModuleRegistry.registerDefaults(MODULES);
        CONFIG.load();

        this.openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.anarchyclient.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        NyanCatGifSpammerModule.registerHud();
    }

    private void onClientTick(final Minecraft client) {
        while (this.openMenuKey.consumeClick()) {
            client.setScreen(new AnarchyClientScreen(MODULES, CONFIG));
        }
        MODULES.tick(client);
    }
}
