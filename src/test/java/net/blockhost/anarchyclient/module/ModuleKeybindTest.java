package net.blockhost.anarchyclient.module;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ModuleKeybindTest {

    @ParameterizedTest
    @CsvSource({"65,4", "82,21", "32,44", "290,58", "301,69", "313,115", "320,98", "330,99", "335,88", "344,229", "347,231"})
    void migratesLegacyGlfwBindings(final int legacyKey, final int scancode) {
        JsonObject json = new JsonObject();
        json.addProperty("key", legacyKey);
        json.addProperty("action", "hold");
        ModuleKeybind binding = ModuleKeybind.unbound();

        binding.fromJson(json);

        assertEquals(scancode, binding.key());
        assertEquals(ModuleBindAction.HOLD, binding.action());
    }

    @ParameterizedTest
    @CsvSource({"-1", "9999"})
    void leavesUnknownLegacyKeysUnbound(final int legacyKey) {
        JsonObject json = new JsonObject();
        json.addProperty("key", legacyKey);
        ModuleKeybind binding = ModuleKeybind.unbound();

        binding.fromJson(json);

        assertFalse(binding.bound());
    }

    @Test
    void keepsScancodesWhenSavingAndLoadingAgain() {
        ModuleKeybind original = new ModuleKeybind(InputConstants.KEY_UP, ModuleBindAction.SMART);
        ModuleKeybind loaded = ModuleKeybind.unbound();

        loaded.fromJson(original.toJson());

        assertEquals(InputConstants.KEY_UP, loaded.key());
        assertEquals(ModuleBindAction.SMART, loaded.action());
        ModuleKeybind reloaded = ModuleKeybind.unbound();
        reloaded.fromJson(loaded.toJson());
        assertEquals(InputConstants.KEY_UP, reloaded.key());
    }

    @Test
    void keepsUnboundBindingsWhenSavingAndLoading() {
        ModuleKeybind loaded = new ModuleKeybind(InputConstants.KEY_A, ModuleBindAction.TOGGLE);
        loaded.fromJson(ModuleKeybind.unbound().toJson());
        assertFalse(loaded.bound());
    }
}
