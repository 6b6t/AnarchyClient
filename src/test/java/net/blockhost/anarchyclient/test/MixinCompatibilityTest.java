package net.blockhost.anarchyclient.test;

import net.fabricmc.loader.api.FabricLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinCompatibilityTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "net.minecraft.client.Camera",
            "net.minecraft.client.DeltaTracker$Timer",
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.MouseHandler",
            "net.minecraft.client.KeyboardHandler",
            "net.minecraft.client.gui.Gui",
            "net.minecraft.client.gui.components.ChatComponent",
            "net.minecraft.client.gui.components.CommandSuggestions",
            "net.minecraft.client.gui.components.EditBox",
            "net.minecraft.client.gui.components.PlayerTabOverlay",
            "net.minecraft.client.gui.screens.ChatScreen",
            "net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl",
            "net.minecraft.client.multiplayer.ClientLevel",
            "net.minecraft.client.multiplayer.ClientPacketListener",
            "net.minecraft.client.multiplayer.MultiPlayerGameMode",
            "net.minecraft.client.multiplayer.resolver.AddressCheck",
            "net.minecraft.client.player.ClientInput",
            "net.minecraft.client.player.KeyboardInput",
            "net.minecraft.client.player.LocalPlayer",
            "net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer",
            "net.minecraft.client.renderer.GameRenderer",
            "net.minecraft.client.renderer.LightmapRenderStateExtractor",
            "net.minecraft.client.renderer.SkyRenderer",
            "net.minecraft.client.renderer.entity.EntityRenderer",
            "net.minecraft.client.renderer.entity.ItemEntityRenderer",
            "net.minecraft.client.renderer.extract.LevelExtractor",
            "net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment",
            "net.minecraft.network.Connection",
            "net.minecraft.world.entity.Entity",
            "net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.entity.player.Player",
            "net.minecraft.world.item.ItemStack"
    })
    void appliesMixinsToMinecraftClasses(final String target) {
        assertTrue(FabricLoader.getInstance().isModLoaded("anarchyclient"), "AnarchyClient must be loaded for mixin validation");
        // Reflection only inspects the loaded class for mixin methods; there is no member to widen.
        Class<?> transformed = assertDoesNotThrow(() -> Class.forName(target, false, MixinCompatibilityTest.class.getClassLoader()));
        assertTrue(
                Arrays.stream(transformed.getDeclaredMethods()).anyMatch(method -> method.getName().contains("anarchyclient$")),
                "AnarchyClient mixins were not applied to " + target);
    }
}
