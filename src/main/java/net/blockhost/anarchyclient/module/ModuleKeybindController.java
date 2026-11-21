package net.blockhost.anarchyclient.module;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

final class ModuleKeybindController {

    private static final int SMART_HOLD_TICKS = 4;

    private final IdentityHashMap<Module, Boolean> previousPressed = new IdentityHashMap<>();
    private final IdentityHashMap<Module, SmartPress> smartPresses = new IdentityHashMap<>();
    // Presses that started while input was allowed. A key typed into chat is never accepted, so its
    // release must not turn off a HOLD module.
    private final Set<Module> acceptedPresses = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Updates one module's bind once per tick.
     *
     * @param pressed    whether the key is down now
     * @param freshPress whether a press that input could accept (no screen open) arrived since the last tick.
     *                   A tap that is already released by now still counts.
     */
    void update(final Module module, final boolean pressed, final boolean freshPress) {
        boolean previous = this.previousPressed.getOrDefault(module, false);
        if (freshPress && !previous) {
            this.acceptedPresses.add(module);
            this.handlePress(module);
            if (!pressed && this.acceptedPresses.remove(module)) {
                // Pressed and released between two ticks.
                this.handleRelease(module);
            }
        } else if (!pressed && previous) {
            if (this.acceptedPresses.remove(module)) {
                this.handleRelease(module);
            }
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
        this.acceptedPresses.remove(module);
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
            case TOGGLE -> {}
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
