package net.blockhost.anarchyclient.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.blockhost.anarchyclient.module.impl.CrystalAuraModule;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class ModuleConfigMigrationTest {

    @Test
    void upgradesOnlyValuesEqualToTheOldDefault() {
        JsonObject moduleJson = moduleJson("""
                {"settings": {"range": 6.0, "other": 7.5, "text": "6.0"}}
                """);

        ModuleConfigMigration.upgradeDefault(moduleJson, "range", 6.0, 12.0);
        ModuleConfigMigration.upgradeDefault(moduleJson, "other", 6.0, 12.0);
        ModuleConfigMigration.upgradeDefault(moduleJson, "text", 6.0, 12.0);
        ModuleConfigMigration.upgradeDefault(moduleJson, "missing", 6.0, 12.0);

        JsonObject settings = moduleJson.getAsJsonObject("settings");
        assertEquals(12.0, settings.get("range").getAsDouble());
        assertEquals(7.5, settings.get("other").getAsDouble());
        assertEquals("6.0", settings.get("text").getAsString());
        assertEquals(3, settings.size());
    }

    @Test
    void ignoresModulesWithoutSettings() {
        JsonObject moduleJson = moduleJson("{\"enabled\": true}");

        ModuleConfigMigration.upgradeDefault(moduleJson, "range", 6.0, 12.0);

        assertEquals(moduleJson("{\"enabled\": true}"), moduleJson);
    }

    @Test
    void crystalAuraUpgradesOldDamageDefaultsAndKeepsCustomValues() {
        JsonObject moduleJson = moduleJson("""
                {"settings": {"min_damage": 4.0, "max_self_damage": 10.0}}
                """);

        new CrystalAuraModule().migrateConfig(moduleJson, 1);

        JsonObject settings = moduleJson.getAsJsonObject("settings");
        assertEquals(6.0, settings.get("min_damage").getAsDouble());
        assertEquals(10.0, settings.get("max_self_damage").getAsDouble());
    }

    @Test
    void doesNotUpgradeConfigsSavedAfterTheDefaultsChanged() {
        // A user who sets the old value again after upgrading keeps it.
        JsonObject moduleJson = moduleJson("""
                {"settings": {"min_damage": 4.0}}
                """);

        new CrystalAuraModule().migrateConfig(moduleJson, ModuleConfigMigration.VANILLA_DAMAGE_DEFAULTS);

        assertEquals(4.0, moduleJson.getAsJsonObject("settings").get("min_damage").getAsDouble());
    }

    @Test
    void treatsUnversionedConfigsAsVersionOne() {
        assertEquals(1, ClientConfig.savedVersion(moduleJson("{\"modules\": {}}")));
        assertEquals(2, ClientConfig.savedVersion(moduleJson("{\"version\": 2, \"modules\": {}}")));
    }

    private static JsonObject moduleJson(final String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
