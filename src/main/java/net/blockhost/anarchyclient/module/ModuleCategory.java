package net.blockhost.anarchyclient.module;

public enum ModuleCategory {
    COMBAT("Combat"),
    RENDER("Render"),
    MOVEMENT("Movement"),
    PLAYER("Player"),
    HUD("HUD"),
    FUN("Fun"),
    MISC("Misc");

    private final String displayName;

    ModuleCategory(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
