package net.blockhost.anarchyclient.module;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.platform.InputConstants;
import org.junit.jupiter.api.Test;

class ModuleKeybindControllerTest {

    @Test
    void toggleBindFlipsOnPressOnly() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(InputConstants.KEY_K);
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
        module.keybind().key(InputConstants.KEY_K);
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
        module.keybind().key(InputConstants.KEY_K);
        module.keybind().action(ModuleBindAction.SMART);

        controller.update(module, true, true);
        controller.update(module, false, true);

        assertTrue(module.enabled());
    }

    @Test
    void smartBindLongPressActsLikeHold() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(InputConstants.KEY_K);
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
    void tapBetweenTwoTicksStillCounts() {
        TestModule toggle = new TestModule();
        toggle.keybind().key(InputConstants.KEY_K);
        toggle.keybind().action(ModuleBindAction.TOGGLE);
        TestModule hold = new TestModule();
        hold.keybind().key(InputConstants.KEY_J);
        hold.keybind().action(ModuleBindAction.HOLD);
        ModuleKeybindController controller = new ModuleKeybindController();

        controller.update(toggle, false, true);
        controller.update(hold, false, true);

        assertTrue(toggle.enabled());
        assertFalse(hold.enabled());
    }

    @Test
    void ignoresNewPressesWhenInputIsBlocked() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(InputConstants.KEY_K);
        module.keybind().action(ModuleBindAction.TOGGLE);

        controller.update(module, true, false);

        assertFalse(module.enabled());
    }

    @Test
    void holdBindIgnoresReleaseOfKeyTypedWhileInputWasBlocked() {
        ModuleKeybindController controller = new ModuleKeybindController();
        TestModule module = new TestModule();
        module.keybind().key(InputConstants.KEY_K);
        module.keybind().action(ModuleBindAction.HOLD);
        module.enabled(true);

        controller.update(module, true, false);
        controller.update(module, false, true);

        assertTrue(module.enabled());
    }

    private static final class TestModule extends Module {

        private TestModule() {
            super("test", "Test", ModuleCategory.MISC, "Test module.");
        }
    }
}
