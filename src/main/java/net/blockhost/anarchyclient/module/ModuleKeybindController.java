package net.blockhost.anarchyclient.module;

import java.util.IdentityHashMap;
import java.util.Map;

final class ModuleKeybindController {

    private static final int SMART_HOLD_TICKS = 4;

    private final Map<Module, Boolean> previousPressed = new IdentityHashMap<>();
    private final Map<Module, SmartPress> smartPresses = new IdentityHashMap<>();

    void update(final Module module, final boolean pressed, final boolean allowNewPresses) {
        boolean previous = this.previousPressed.getOrDefault(module, false);
        if (pressed && !previous && allowNewPresses) {
            this.handlePress(module);
        } else if (!pressed && previous) {
            this.handleRelease(module);
        } else if (pressed) {
            SmartPress smartPress = this.smartPresses.get(module);
            if (smartPress != null) {
                smartPress.tick();
            }
        }
        this.previousPressed.put(module, pressed);
    }

    void clear(final Module module) {
        this.previousPressed.remove(module);
        this.smartPresses.remove(module);
    }

    private void handlePress(final Module module) {
        switch (module.keybind().action()) {
            case TOGGLE -> module.toggle();
            case HOLD -> module.enabled(true);
            case SMART -> {
                this.smartPresses.put(module, new SmartPress(module.enabled()));
                module.enabled(true);
            }
        }
    }

    private void handleRelease(final Module module) {
        switch (module.keybind().action()) {
            case HOLD -> module.enabled(false);
            case SMART -> {
                SmartPress press = this.smartPresses.remove(module);
                if (press != null) {
                    module.enabled(press.ticksHeld() >= SMART_HOLD_TICKS ? false : !press.wasEnabled());
                }
            }
            case TOGGLE -> {
            }
        }
    }

    private static final class SmartPress {

        private final boolean wasEnabled;
        private int ticksHeld;

        private SmartPress(final boolean wasEnabled) {
            this.wasEnabled = wasEnabled;
        }

        private void tick() {
            this.ticksHeld++;
        }

        private boolean wasEnabled() {
            return this.wasEnabled;
        }

        private int ticksHeld() {
            return this.ticksHeld;
        }
    }
}
