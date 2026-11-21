package net.blockhost.anarchyclient.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ServerPortScannerTest {

    @Test
    void validatesPortScanRanges() {
        ServerPortScanner.PortRange range = ServerPortScanner.range(25570, 25565);

        assertEquals(25565, range.min());
        assertEquals(25570, range.max());
        assertEquals(6, range.checks().size());
        assertThrows(
                IllegalArgumentException.class, () -> ServerPortScanner.range(1, ServerPortScanner.MAX_RANGE_SIZE + 2));
    }

    @Test
    void formatsOpenPortScanResults() {
        assertEquals(
                "No open ports found.",
                ServerPortScanner.format(List.of(new ServerPortScanner.PortResult(25565, "Minecraft", false))));
        assertEquals(
                "Open ports: 25565 (Minecraft), 8123 (Dynmap)",
                ServerPortScanner.format(List.of(
                        new ServerPortScanner.PortResult(25565, "Minecraft", true),
                        new ServerPortScanner.PortResult(22, "SSH", false),
                        new ServerPortScanner.PortResult(8123, "Dynmap", true))));
        assertEquals(
                "25565,8123",
                ServerPortScanner.openPortList(List.of(
                        new ServerPortScanner.PortResult(25565, "Minecraft", true),
                        new ServerPortScanner.PortResult(22, "SSH", false),
                        new ServerPortScanner.PortResult(8123, "Dynmap", true))));
    }
}
