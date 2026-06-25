package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.util.PlaceholderFormatter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class MeteorRejectsPlanModulesTest {

    @Test
    void chatBotParsesCommandsAndReplies() {
        assertEquals("ping", ChatBotModule.command("<Steve> !ping now", "!"));
        assertEquals(Map.of("ping", "Pong", "coords", "{player.pos}"),
                ChatBotModule.parseReplies("ping=Pong\\ncoords={player.pos}"));
        assertEquals("Hello Steve", PlaceholderFormatter.format("Hello {player}", Map.of("player", "Steve")));
    }

    @Test
    void packetAndSpoofFiltersAreCaseInsensitive() {
        assertTrue(CustomPacketsModule.matches("minecraft:brand brand=Paper", "PAPER"));
        assertFalse(CustomPacketsModule.matches("minecraft:brand brand=Paper", "velocity"));
        assertTrue(BungeeCordSpoofModule.matches("play.example.org", "EXAMPLE"));
        assertFalse(BungeeCordSpoofModule.matches("play.example.org", "other"));
    }

    @Test
    void movementHelpersStayDeterministic() {
        assertEquals(12.125, RoboWalkModule.snap(12.13, 0.125), 1.0E-9);
        assertEquals(90.0F, RenderingModule.clamp(100.0F, 30.0F, 90.0F));
        assertFalse(ArrowDamageModule.isSupported(ItemStack.EMPTY, true));
    }

    @Test
    void oreSimRequiresSeedAndCapsMarkers() {
        assertTrue(OreSimModule.scan(new BlockPos(0, 64, 0), "", "Diamond", 16, 16).isEmpty());

        List<BlockPos> markers = OreSimModule.scan(new BlockPos(0, 64, 0), "12345", "Diamond", 16, 8);

        assertTrue(markers.size() <= 8);
        assertEquals(markers, OreSimModule.scan(new BlockPos(0, 64, 0), "12345", "Diamond", 16, 8));
    }
}
