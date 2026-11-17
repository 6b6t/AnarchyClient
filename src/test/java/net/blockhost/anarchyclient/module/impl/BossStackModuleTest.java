package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class BossStackModuleTest {

    @Test
    void listsReceivedBossBars() {
        BossStackModule module = moduleWithBoss();

        assertEquals(List.of("Wither 100%"), module.lines(null));
    }

    @Test
    void forgetsBossBarsAfterLeavingTheServer() {
        BossStackModule module = moduleWithBoss();

        module.gameLeft(null, null);

        assertEquals(List.of(), module.lines(null));
    }

    @Test
    void forgetsBossBarsWhenDisabled() {
        BossStackModule module = moduleWithBoss();

        module.enabled(false);

        assertEquals(List.of(), module.lines(null));
    }

    private static BossStackModule moduleWithBoss() {
        BossStackModule module = new BossStackModule();
        module.enabled(true);
        ServerBossEvent boss = new ServerBossEvent(UUID.randomUUID(), Component.literal("Wither"),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
        module.receivePacket(null, null, ClientboundBossEventPacket.createAddPacket(boss));
        return module;
    }
}
