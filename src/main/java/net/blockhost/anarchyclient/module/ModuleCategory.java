package net.blockhost.anarchyclient.module;

import java.util.Locale;
import net.blockhost.anarchyclient.i18n.Translations;

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
        return Translations.text(this.translationKey(), this.displayName);
    }

    public String englishName() {
        return this.displayName;
    }

    public String translationKey() {
        return "category.anarchyclient." + this.name().toLowerCase(Locale.ROOT);
    }
}
