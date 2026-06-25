package net.blockhost.anarchyclient.friends;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendManagerTest {

    @TempDir
    private Path tempDir;

    @Test
    void parsesNamesFromCommonSeparators() {
        assertEquals(List.of("Alex", "Steve", "enderman"), FriendManager.parseNames(" Alex, Steve | enderman  "));
    }

    @Test
    void ignoresBlankFriendLists() {
        assertEquals(List.of(), FriendManager.parseNames("  | ,  "));
    }

    @Test
    void matchesFriendsCaseInsensitively() {
        FriendManager friends = new FriendManager(this.tempDir.resolve("friends.txt"));

        assertTrue(friends.add("Alex"));

        assertTrue(friends.isFriend("alex"));
        assertTrue(friends.isFriend("ALEX"));
        assertFalse(friends.isFriend("Steve"));
        assertEquals(List.of("Alex"), friends.friends());
    }

    @Test
    void persistsFriendsAcrossReloads() {
        Path path = this.tempDir.resolve("friends.txt");
        FriendManager saved = new FriendManager(path);
        saved.add("Alex");
        saved.add("Steve");

        FriendManager loaded = new FriendManager(path);
        loaded.load();

        assertTrue(loaded.isFriend("alex"));
        assertTrue(loaded.isFriend("steve"));
        assertEquals(List.of("Alex", "Steve"), loaded.friends());
    }
}
