package net.blockhost.anarchyclient.config;

import com.google.gson.JsonObject;

public interface ModuleConfigMigration {

    void migrateConfig(JsonObject moduleJson);
}
