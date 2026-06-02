package net.blockhost.anarchyclient.module;

public enum ModuleCategory {
    COMBAT("Combat"),
    MISC("Misc");

    private final String displayName;

    ModuleCategory(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
