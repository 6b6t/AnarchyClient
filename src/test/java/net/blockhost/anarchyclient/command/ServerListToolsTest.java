package net.blockhost.anarchyclient.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerListToolsTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesServerListExportsConservatively() throws Exception {
        Path path = this.tempDir.resolve("servers.json");
        Files.writeString(path, """
                [
                  {"name":"Main","address":"Example.ORG:25565"},
                  {"name":"","address":" "}
                ]
                """, StandardCharsets.UTF_8);

        List<ServerListTools.ServerEntry> entries = ServerListTools.readJson(path);

        assertEquals(1, entries.size());
        assertEquals("Main", entries.getFirst().name());
        assertEquals("example.org:25565", entries.getFirst().address());
        assertEquals("", ServerListTools.normalizeAddress(null));
    }
}
