package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.util.PlaceholderFormatter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
    void oreSimUsesVanillaGenerationHeights() {
        assertEquals(new OreSimModule.OreBand(Level.OVERWORLD, -64, -59, 16), OreSimModule.band("Diamond"));
        // Small debris veins spread from Y 8 to 119; large veins peak at Y 16.
        assertEquals(new OreSimModule.OreBand(Level.NETHER, 8, 16, 119), OreSimModule.band("Ancient Debris"));
    }
}
