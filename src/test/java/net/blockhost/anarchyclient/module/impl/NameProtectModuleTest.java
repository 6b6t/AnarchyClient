package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class NameProtectModuleTest {

    @Test
    void replacesWholeNamesOnly() {
        assertEquals("You said hi. Also Alice", NameProtectModule.replace("Al said hi. Also Alice", "Al", "You"));
        assertEquals("<You> hi", NameProtectModule.replace("<Al> hi", "Al", "You"));
    }

    @Test
    void keepsTheStyleOfEachPart() {
        Component message = Component.empty()
                .append(Component.literal("[Admin] ").withStyle(ChatFormatting.RED))
                .append(Component.literal("Steve").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" joined"));

        Component replaced = NameProtectModule.replaceInComponent(message, List.of("Steve"), "You");

        assertEquals("[Admin] You joined", replaced.getString());
        List<String> colors = new ArrayList<>();
        replaced.visit(
                (style, text) -> {
                    colors.add(text + "=" + style.getColor());
                    return Optional.empty();
                },
                Style.EMPTY);
        assertEquals(List.of("[Admin] =red", "You=gold", " joined=null"), colors);
    }
}
