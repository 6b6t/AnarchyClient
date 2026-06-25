package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class MeteorExpansionModulesTest {

    @Test
    void registersMeteorInspiredExpansionModules() {
        ModuleManager modules = new ModuleManager();

        ModuleRegistry.registerDefaults(modules);

        for (String id : List.of(
                "exp_thrower",
                "attribute_swap",
                "wall_hack",
                "air_jump",
                "anchor",
                "bounce",
                "flight",
                "slippy",
                "timer",
                "collisions",
                "click_tp",
                "no_interact",
                "ghost_hand",
                "reach",
                "rotation",
                "no_rotate_set",
                "rendering",
                "better_beacons",
                "blur",
                "entity_owner",
                "item_physics",
                "text_hud",
                "item_hud",
                "map_hud",
                "player_model_hud",
                "hole_hud",
                "infinity_miner",
                "flamethrower",
                "server_spoof",
                "message_aura",
                "book_bot",
                "notebot",
                "discord_presence",
                "fake_player",
                "swarm"
        )) {
            assertTrue(modules.find(id).isPresent(), "missing module " + id);
        }

        assertEquals("middle_click_action", modules.find("middle_click_extra").orElseThrow().id());
        assertEquals("multi_actions", modules.find("multitask").orElseThrow().id());
        assertEquals("chat_spammer", modules.find("spam").orElseThrow().id());
        assertEquals("hitbox", modules.find("hitboxes").orElseThrow().id());
        assertEquals("auto_sprint", modules.find("sprint").orElseThrow().id());
    }

    @Test
    void notebotParsesNotesAndCalculatesPitch() {
        assertEquals(0, NotebotModule.parseNote("bad"));
        assertEquals(0, NotebotModule.parseNote("-4"));
        assertEquals(24, NotebotModule.parseNote("32"));
        assertEquals(1.0F, NotebotModule.pitch(12), 1.0E-6F);
        assertEquals(2.0F, NotebotModule.pitch(24), 1.0E-6F);
        assertEquals(0.5F, NotebotModule.pitch(0), 1.0E-6F);
    }

    @Test
    void betterBeaconsAddsBeaconOnlyTooltipDetails() {
        BetterBeaconsModule module = new BetterBeaconsModule();
        List<Component> beacon = new java.util.ArrayList<>();
        List<Component> dirt = new java.util.ArrayList<>();

        BetterBeaconsModule.addBeaconTooltip(true, beacon);
        BetterBeaconsModule.addBeaconTooltip(false, dirt);

        assertEquals(2, beacon.size());
        assertTrue(beacon.getFirst().getString().contains("netherite"));
        assertTrue(beacon.get(1).getString().contains("50 blocks"));
        assertTrue(dirt.isEmpty());
    }

    @Test
    void serverSpoofRewritesOnlyLoginHandshakeHost() {
        ServerSpoofModule module = new ServerSpoofModule();
        StringSetting host = stringSetting(module, "host");
        host.value("spoof.example");

        Packet<?> rewritten = module.replaceSendPacket(null, null,
                new ClientIntentionPacket(999, "real.example", 25565, ClientIntent.LOGIN));
        Packet<?> status = module.replaceSendPacket(null, null,
                new ClientIntentionPacket(999, "real.example", 25565, ClientIntent.STATUS));

        ClientIntentionPacket login = assertInstanceOf(ClientIntentionPacket.class, rewritten);
        ClientIntentionPacket unchanged = assertInstanceOf(ClientIntentionPacket.class, status);
        assertEquals("spoof.example", login.hostName());
        assertEquals("real.example", unchanged.hostName());
    }

    @Test
    void reachAndStatusModulesExposeConfiguredState() {
        ReachModule reach = new ReachModule();
        reach.enabled(true);
        reach.tick(null);
        assertEquals(1.0, ReachModule.blockBonus());
        assertEquals(1.0, ReachModule.entityBonus());
        reach.enabled(false);
        assertEquals(0.0, ReachModule.blockBonus());
        assertEquals(0.0, ReachModule.entityBonus());

        DiscordPresenceModule discord = new DiscordPresenceModule();
        assertEquals("Playing AnarchyClient", discord.status());

        SwarmModule swarm = new SwarmModule();
        assertEquals("Worker:local", swarm.status());
    }

    private static StringSetting stringSetting(final Module module, final String id) {
        return module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .filter(StringSetting.class::isInstance)
                .map(StringSetting.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
