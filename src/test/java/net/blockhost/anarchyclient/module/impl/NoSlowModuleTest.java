package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class NoSlowModuleTest {

    private final NoSlowModule module = new NoSlowModule();

    @AfterEach
    void disable() {
        this.module.enabled(false);
    }

    @Test
    void vanillaSlowsOnSoulSandAndHoneyThroughTheBlockSpeedFactor() {
        assertEquals(0.4F, NoSlowModule.speedFactor(Blocks.SOUL_SAND, true));
        assertEquals(0.4F, NoSlowModule.speedFactor(Blocks.HONEY_BLOCK, true));
    }

    @Test
    void enabledModuleRemovesTheSpeedFactorForTheLocalPlayer() {
        this.module.enabled(true);

        assertEquals(1.0F, NoSlowModule.speedFactor(Blocks.SOUL_SAND, true));
        assertEquals(1.0F, NoSlowModule.speedFactor(Blocks.HONEY_BLOCK, true));
        assertEquals(0.4F, NoSlowModule.speedFactor(Blocks.SOUL_SAND, false));
        assertTrue(NoSlowModule.shouldIgnoreStuckBlock(Blocks.SLIME_BLOCK.defaultBlockState()));
    }

    @Test
    void disabledBlockOptionKeepsTheSlowdown() {
        this.module.enabled(true);
        this.booleanSetting("soul_sand").value(false);

        assertEquals(0.4F, NoSlowModule.speedFactor(Blocks.SOUL_SAND, true));
        assertEquals(1.0F, NoSlowModule.speedFactor(Blocks.HONEY_BLOCK, true));
    }

    private BooleanSetting booleanSetting(final String id) {
        return this.module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .map(BooleanSetting.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
