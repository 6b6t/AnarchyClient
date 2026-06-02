package net.blockhost.anarchyclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerTest {

    @Test
    void rejectsDuplicateModuleIds() {
        ModuleManager manager = new ModuleManager();
        manager.register(new TestModule("test", ModuleCategory.COMBAT));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.register(new TestModule("test", ModuleCategory.FUN))
        );

        assertEquals("Duplicate module id: test", exception.getMessage());
    }

    @Test
    void filtersModulesByCategoryInRegistrationOrder() {
        ModuleManager manager = new ModuleManager();
        Module combatA = new TestModule("combat_a", ModuleCategory.COMBAT);
        Module fun = new TestModule("fun", ModuleCategory.FUN);
        Module combatB = new TestModule("combat_b", ModuleCategory.COMBAT);

        manager.register(combatA);
        manager.register(fun);
        manager.register(combatB);

        assertEquals(List.of(combatA, combatB), manager.byCategory(ModuleCategory.COMBAT));
        assertEquals(Optional.of(fun), manager.find("fun"));
    }

    @Test
    void dispatchesHooksOnlyToEnabledModules() {
        ModuleManager manager = new ModuleManager();
        TestModule enabled = new TestModule("enabled", ModuleCategory.HUD);
        TestModule disabled = new TestModule("disabled", ModuleCategory.HUD);
        manager.register(enabled);
        manager.register(disabled);
        enabled.enabled(true);

        manager.tick(null);
        manager.renderWorld(null);
        manager.renderHud(null, null);

        assertEquals(1, enabled.ticks);
        assertEquals(1, enabled.worldRenders);
        assertEquals(1, enabled.hudRenders);
        assertEquals(0, disabled.ticks);
        assertEquals(0, disabled.worldRenders);
        assertEquals(0, disabled.hudRenders);
    }

    private static final class TestModule extends Module {

        private int ticks;
        private int worldRenders;
        private int hudRenders;

        private TestModule(final String id, final ModuleCategory category) {
            super(id, id, category);
        }

        @Override
        public void tick(final Minecraft client) {
            this.ticks++;
        }

        @Override
        public void renderWorld(final LevelRenderContext context) {
            this.worldRenders++;
        }

        @Override
        public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
            this.hudRenders++;
        }
    }
}
