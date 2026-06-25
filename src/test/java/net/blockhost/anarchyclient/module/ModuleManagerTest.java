package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.event.GameJoinedEvent;
import net.blockhost.anarchyclient.event.GameLeftEvent;
import net.blockhost.anarchyclient.event.PacketReceiveEvent;
import net.blockhost.anarchyclient.event.PacketSendEvent;
import net.blockhost.anarchyclient.event.PacketSentEvent;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        manager.updateInput(null, new ClientInput());
        manager.renderWorld(null);
        manager.renderHud(null, null);
        manager.call(new PacketSentEvent(null, null, null));
        manager.call(new GameJoinedEvent(null, null));
        manager.call(new GameLeftEvent(null, null));

        assertEquals(1, enabled.ticks);
        assertEquals(1, enabled.inputUpdates);
        assertEquals(1, enabled.worldRenders);
        assertEquals(1, enabled.hudRenders);
        assertEquals(1, enabled.packetsSent);
        assertEquals(1, enabled.gameJoins);
        assertEquals(1, enabled.gameLeaves);
        assertEquals(0, disabled.ticks);
        assertEquals(0, disabled.inputUpdates);
        assertEquals(0, disabled.worldRenders);
        assertEquals(0, disabled.hudRenders);
        assertEquals(0, disabled.packetsSent);
        assertEquals(0, disabled.gameJoins);
        assertEquals(0, disabled.gameLeaves);
    }

    @Test
    void returnsTrueWhenEnabledModulePreventsEdgeFall() {
        ModuleManager manager = new ModuleManager();
        TestModule enabled = new TestModule("enabled", ModuleCategory.MOVEMENT);
        TestModule disabled = new TestModule("disabled", ModuleCategory.MOVEMENT);
        manager.register(enabled);
        manager.register(disabled);
        enabled.preventEdgeFall = true;
        disabled.preventEdgeFall = true;
        enabled.enabled(true);

        assertTrue(manager.preventEdgeFall(null, null));

        enabled.enabled(false);
        assertFalse(manager.preventEdgeFall(null, null));
    }

    @Test
    void cancellablePacketHooksUseModuleReturnValues() {
        ModuleManager manager = new ModuleManager();
        TestModule module = new TestModule("packets", ModuleCategory.MISC);
        manager.register(module);
        module.enabled(true);

        PacketReceiveEvent receiveAllowed = manager.call(new PacketReceiveEvent(null, null, null));
        PacketSendEvent sendAllowed = manager.call(new PacketSendEvent(null, null, null));

        assertFalse(receiveAllowed.isCancelled());
        assertFalse(sendAllowed.isCancelled());
        assertEquals(1, module.packetsReceived);
        assertEquals(1, module.packetsSending);

        module.cancelPackets = true;
        PacketReceiveEvent receiveCancelled = manager.call(new PacketReceiveEvent(null, null, null));
        PacketSendEvent sendCancelled = manager.call(new PacketSendEvent(null, null, null));

        assertTrue(receiveCancelled.isCancelled());
        assertTrue(sendCancelled.isCancelled());
    }

    @Test
    void keepsFlatSettingsViewWhileSupportingGroups() {
        GroupedModule module = new GroupedModule();

        assertEquals(List.of(module.generalSetting, module.combatSetting), module.settings());
        assertEquals("General switch", module.generalSetting.description());
        assertEquals(2, module.settingGroups().size());
        assertEquals("General", module.settingGroups().getFirst().name());
        assertEquals(List.of(module.generalSetting), module.settingGroups().getFirst().settings());
        assertEquals("Combat", module.settingGroups().get(1).name());
        assertEquals(List.of(module.combatSetting), module.settingGroups().get(1).settings());
    }

    private static final class TestModule extends Module {

        private int ticks;
        private int inputUpdates;
        private int worldRenders;
        private int hudRenders;
        private int packetsReceived;
        private int packetsSending;
        private int packetsSent;
        private int gameJoins;
        private int gameLeaves;
        private boolean preventEdgeFall;
        private boolean cancelPackets;

        private TestModule(final String id, final ModuleCategory category) {
            super(id, id, category);
        }

        @Override
        public void tick(final Minecraft client) {
            this.ticks++;
        }

        @Override
        public void updateInput(final Minecraft client, final ClientInput input) {
            this.inputUpdates++;
        }

        @Override
        public boolean preventEdgeFall(final Minecraft client, final Player player) {
            return this.preventEdgeFall;
        }

        @Override
        public void renderWorld(final LevelRenderContext context) {
            this.worldRenders++;
        }

        @Override
        public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
            this.hudRenders++;
        }

        @Override
        public boolean receivePacket(final Minecraft client, final net.minecraft.network.Connection connection,
                                     final net.minecraft.network.protocol.Packet<?> packet) {
            this.packetsReceived++;
            return this.cancelPackets;
        }

        @Override
        public boolean sendPacket(final Minecraft client, final net.minecraft.network.Connection connection,
                                  final net.minecraft.network.protocol.Packet<?> packet) {
            this.packetsSending++;
            return this.cancelPackets;
        }

        @Override
        public void sentPacket(final Minecraft client, final net.minecraft.network.Connection connection,
                               final net.minecraft.network.protocol.Packet<?> packet) {
            this.packetsSent++;
        }

        @Override
        public void gameJoined(final Minecraft client, final net.minecraft.client.multiplayer.ClientPacketListener listener) {
            this.gameJoins++;
        }

        @Override
        public void gameLeft(final Minecraft client, final net.minecraft.client.multiplayer.ClientPacketListener listener) {
            this.gameLeaves++;
        }
    }

    private static final class GroupedModule extends Module {

        private final BooleanSetting generalSetting = this.setting(BooleanSetting.from(BooleanSetting.builder()
                .id("general")
                .name("General")
                .defaultValue(true)
                .description("General switch")
                .build()));
        private final SettingGroup combat = this.settingGroup("combat", "Combat");
        private final BooleanSetting combatSetting = this.setting(this.combat, BooleanSetting.from(BooleanSetting.builder()
                .id("combat")
                .name("Combat")
                .defaultValue(false)
                .build()));

        private GroupedModule() {
            super("grouped", "Grouped", ModuleCategory.COMBAT);
        }
    }
}
