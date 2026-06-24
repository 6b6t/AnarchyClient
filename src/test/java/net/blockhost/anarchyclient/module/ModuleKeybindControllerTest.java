package net.blockhost.anarchyclient.module;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleKeybindControllerTest {

    @Test
    void toggleBindFlipsOnPressOnly() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(GLFW.GLFW_KEY_K);
        module.keybind().action(ModuleBindAction.TOGGLE);

        controller.update(module, true, true);
        controller.update(module, true, true);
        controller.update(module, false, true);

        assertTrue(module.enabled());
    }

    @Test
    void holdBindDisablesOnRelease() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(GLFW.GLFW_KEY_K);
        module.keybind().action(ModuleBindAction.HOLD);

        controller.update(module, true, true);
        assertTrue(module.enabled());

        controller.update(module, false, true);
        assertFalse(module.enabled());
    }

    @Test
    void smartBindQuickPressTogglesFromOriginalState() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(GLFW.GLFW_KEY_K);
        module.keybind().action(ModuleBindAction.SMART);

        controller.update(module, true, true);
        controller.update(module, false, true);

        assertTrue(module.enabled());
    }

    @Test
    void smartBindLongPressActsLikeHold() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(GLFW.GLFW_KEY_K);
        module.keybind().action(ModuleBindAction.SMART);

        controller.update(module, true, true);
        controller.update(module, true, true);
        controller.update(module, true, true);
        controller.update(module, true, true);
        controller.update(module, true, true);
        controller.update(module, false, true);

        assertFalse(module.enabled());
    }

    @Test
    void ignoresNewPressesWhenInputIsBlocked() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(GLFW.GLFW_KEY_K);
        module.keybind().action(ModuleBindAction.TOGGLE);

        controller.update(module, true, false);

        assertFalse(module.enabled());
    }

    private static final class TestModule extends Module {

        private TestModule() {
            super("test", "Test", ModuleCategory.MISC);
        }
    }
}
