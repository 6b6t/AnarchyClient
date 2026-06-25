package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegistryListSettingTest {

    @Test
    void blockListParsesNamespacedAndDefaultIds() {
        BlockListSetting setting = BlockListSetting.from(BlockListSetting.builder()
                .id("blocks")
                .name("Blocks")
                .defaultValue(List.of(Blocks.STONE))
                .build());

        setting.valueFromString("dirt,minecraft:stone,missing_block");

        assertEquals(List.of("minecraft:dirt", "minecraft:stone"), setting.ids());
        assertEquals("minecraft:dirt,minecraft:stone", setting.valueString());
    }

    @Test
    void blockListReadsJsonArraysAndLegacyStrings() {
        BlockListSetting setting = BlockListSetting.from(BlockListSetting.builder()
                .id("blocks")
                .name("Blocks")
                .build());
        JsonArray array = new JsonArray();
        array.add("minecraft:obsidian");
        array.add("bedrock");

        setting.fromJson(array);
        assertEquals(List.of("minecraft:obsidian", "minecraft:bedrock"), setting.ids());

        setting.fromJson(new JsonPrimitive("stone;dirt"));
        assertEquals(List.of("minecraft:stone", "minecraft:dirt"), setting.ids());
    }
}
