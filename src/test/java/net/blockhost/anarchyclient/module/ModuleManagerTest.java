package net.blockhost.anarchyclient.module;

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
                () -> manager.register(new TestModule("test", ModuleCategory.MISC))
        );

        assertEquals("Duplicate module id: test", exception.getMessage());
    }

    @Test
    void filtersModulesByCategoryInRegistrationOrder() {
        ModuleManager manager = new ModuleManager();
        Module combatA = new TestModule("combat_a", ModuleCategory.COMBAT);
        Module misc = new TestModule("misc", ModuleCategory.MISC);
        Module combatB = new TestModule("combat_b", ModuleCategory.COMBAT);

        manager.register(combatA);
        manager.register(misc);
        manager.register(combatB);

        assertEquals(List.of(combatA, combatB), manager.byCategory(ModuleCategory.COMBAT));
        assertEquals(Optional.of(misc), manager.find("misc"));
    }

    private static final class TestModule extends Module {

        private TestModule(final String id, final ModuleCategory category) {
            super(id, id, category);
        }
    }
}
