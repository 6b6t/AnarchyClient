package net.blockhost.anarchyclient.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Lets a module rewrite its stored JSON before the settings load. */
public interface ModuleConfigMigration {

    /** Config format version where combat damage defaults were rescaled for vanilla explosion damage. */
    int VANILLA_DAMAGE_DEFAULTS = 2;

    /**
     * Migrates one module's saved JSON before it loads.
     *
     * @param savedVersion the config format version the file was written with ({@code 1} for files that
     *                     predate versioning). Use it so a migration runs once and never overrides a value the
     *                     user chose after upgrading.
     */
    void migrateConfig(JsonObject moduleJson, int savedVersion);

    /**
     * Replaces a stored number that still equals {@code oldDefault} with {@code newDefault}, so configs
     * saved before a default changed pick up the new default. Any other stored value is kept.
     */
    static void upgradeDefault(final JsonObject moduleJson, final String settingId, final double oldDefault,
                               final double newDefault) {
        if (!(moduleJson.get("settings") instanceof JsonObject settings)) {
            return;
        }
        JsonElement value = settings.get(settingId);
        if (value instanceof JsonPrimitive primitive && primitive.isNumber() && primitive.getAsDouble() == oldDefault) {
            settings.addProperty(settingId, newDefault);
        }
    }
}
