package net.blockhost.anarchyclient.server;

import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerObserverTest {

    @Test
    void normalizesRootDomains() {
        assertEquals("example.org", ServerObserver.rootDomain("play.example.org:25565"));
        assertEquals("example.org", ServerObserver.rootDomain("example.org"));
    }

    @Test
    void guessesKnownAntiCheatTransactionPatterns() {
        assertEquals(Optional.of("Watchdog"), ServerObserver.guessAntiCheat("mc.hypixel.net", List.of(1, 2, 3, 4, 5)));
        assertEquals(Optional.of("Vulcan"), ServerObserver.guessAntiCheat("test.local", List.of(-23770, -23769, -23768, -23767, -23766)));
        assertEquals(Optional.of("Matrix"), ServerObserver.guessAntiCheat("test.local", List.of(100, 101, 102, 103, 104)));
        assertEquals(Optional.of("Grim"), ServerObserver.guessAntiCheat("test.local", List.of(-1, -2, -3, -4, -5)));
        assertEquals(Optional.of("Intave"), ServerObserver.guessAntiCheat("test.local", List.of(-4000, -4001, -4002, -4003, -4004)));
        assertEquals(Optional.empty(), ServerObserver.guessAntiCheat("test.local", List.of(1, 2, 3, 4)));
    }

    @Test
    void extractsPluginNamesFromCommandSuggestions() {
        Set<String> plugins = ServerObserver.extractPluginsFromSuggestions(List.of(
                new ClientboundCommandSuggestionsPacket.Entry("bukkit:plugins", Optional.empty()),
                new ClientboundCommandSuggestionsPacket.Entry("/grimac:alerts", Optional.empty()),
                new ClientboundCommandSuggestionsPacket.Entry("help", Optional.empty())
        ));

        assertEquals(Set.of("bukkit", "grimac"), plugins);
    }

    @Test
    void extractsPluginNamesFromChatMessages() {
        assertEquals(Set.of("grimac", "viaversion", "worldedit"),
                ServerObserver.extractPluginsFromMessage("Plugins (3): GrimAC, ViaVersion, WorldEdit"));
    }
}
