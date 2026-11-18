package net.blockhost.anarchyclient.server;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerProfileStoreTest {

    @Test
    void anEmptyFingerprintKeepsWhatEarlierSessionsFound() {
        ServerProfileStore.ServerProfile profile = new ServerProfileStore.ServerProfile();
        profile.merge(new ServerObserver.Fingerprint("example.net", "Grim", Set.of("paper"), Set.of("LuckPerms"),
                Set.of("minecraft:brand"), 40));

        profile.merge(new ServerObserver.Fingerprint("example.net", "", Set.of(), Set.of("WorldGuard"), Set.of(), 10));

        assertEquals("Grim", profile.antiCheat);
        assertEquals(Set.of("LuckPerms", "WorldGuard"), profile.knownPlugins);
        assertEquals(Set.of("minecraft:brand"), profile.payloadChannels);
        assertEquals(Set.of("paper"), profile.environmentLabels);
        assertEquals(10, profile.lastSafetyScore);
    }
}
