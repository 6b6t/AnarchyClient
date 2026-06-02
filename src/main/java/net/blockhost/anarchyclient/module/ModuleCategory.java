package net.blockhost.anarchyclient.module;

public enum ModuleCategory {
    COMBAT("Combat"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    WORLD("World"),
    PLAYER("Player"),
    HUD("HUD"),
    MISC("Misc"),
    FUN("Fun");

    private final String displayName;

    ModuleCategory(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
