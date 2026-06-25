package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextValueSettingsTest {

    @Test
    void stringListsParseTextAndJsonWithStableDeduplication() {
        StringListSetting setting = StringListSetting.from(StringListSetting.builder()
                .id("messages")
                .name("Messages")
                .defaultValue(List.of("default"))
                .suggestions(List.of("z", "a", "z"))
                .build());

        setting.valueFromString("one, two;two three");

        assertEquals(List.of("one", "two", "three"), setting.value());
        assertEquals("one,two,three", setting.valueString());
        assertEquals(List.of("a", "z"), setting.suggestions());

        JsonArray array = new JsonArray();
        array.add("four five");
        array.add("six");
        setting.fromJson(array);

        assertEquals(List.of("four", "five", "six"), setting.value());
    }

    @Test
    void blockPositionsParseTextAndObjectJson() {
        BlockPosSetting setting = BlockPosSetting.from(BlockPosSetting.builder()
                .id("target")
                .name("Target")
                .build());

        setting.valueFromString("12 64 -5");

        assertEquals(new BlockPos(12, 64, -5), setting.value());
        assertEquals("12,64,-5", setting.valueString());

        JsonObject object = new JsonObject();
        object.addProperty("x", -1);
        object.addProperty("y", 70);
        object.addProperty("z", 3);
        setting.fromJson(object);

        assertEquals(new BlockPos(-1, 70, 3), setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.valueFromString("1,2"));
    }

    @Test
    void vectorsParseTextAndObjectJson() {
        Vector3dSetting setting = Vector3dSetting.from(Vector3dSetting.builder()
                .id("velocity")
                .name("Velocity")
                .build());

        setting.valueFromString("1.5, -2, 3.25");

        assertEquals(new Vec3(1.5, -2.0, 3.25), setting.value());
        assertEquals("1.5,-2.0,3.25", setting.valueString());

        JsonObject object = new JsonObject();
        object.addProperty("x", 0.25);
        object.addProperty("y", 1.0);
        object.addProperty("z", -4.5);
        setting.fromJson(object);

        assertEquals(new Vec3(0.25, 1.0, -4.5), setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.valueFromString("north"));
    }

    @Test
    void colorsParseHexChannelsAndLists() {
        ColorSetting setting = ColorSetting.from(ColorSetting.builder()
                .id("color")
                .name("Color")
                .defaultValue(SettingColor.rgb(255, 255, 255))
                .build());

        setting.valueFromString("#33669980");

        assertEquals(new SettingColor(0x33, 0x66, 0x99, 0x80), setting.value());
        assertEquals("#33669980", setting.valueString());
        assertEquals(0x80336699, setting.value().argb());
        assertEquals("#ff0080", SettingColor.parse("300, -4, 128").hex());

        ColorListSetting list = ColorListSetting.from(ColorListSetting.builder()
                .id("palette")
                .name("Palette")
                .build());
        list.valueFromString("#ff0000,#00ff00 0,0,255,64 #ff0000");

        assertEquals(List.of(
                SettingColor.rgb(255, 0, 0),
                SettingColor.rgb(0, 255, 0),
                new SettingColor(0, 0, 255, 64)
        ), list.value());
        assertEquals("#ff0000,#00ff00,#0000ff40", list.valueString());
        assertThrows(IllegalArgumentException.class, () -> setting.valueFromString("not-a-color"));
    }
}
