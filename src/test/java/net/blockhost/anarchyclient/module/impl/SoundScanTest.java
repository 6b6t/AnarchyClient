package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class SoundScanTest {

    @Test
    void parsesKnownSoundIdsAndDropsUnknownOnes() {
        assertEquals(
                Set.of(
                        Identifier.withDefaultNamespace("entity.generic.explode"),
                        Identifier.withDefaultNamespace("block.chest.open")),
                SoundScan.parseSoundIds("Entity.Generic.Explode, minecraft:block.chest.open | not.a.sound"));
    }

    @Test
    void returnsEmptySetForBlankInput() {
        assertEquals(Set.of(), SoundScan.parseSoundIds(null));
        assertEquals(Set.of(), SoundScan.parseSoundIds("  "));
    }
}
