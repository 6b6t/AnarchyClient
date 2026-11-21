package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.authlib.properties.Property;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BungeeCordSpoofModuleTest {

    private static final UUID ID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void forwardsHostIpAndUndashedUuid() {
        assertEquals(
                "play.example.org\u0000127.0.0.1\u0000069a79f444e94726a5befca90e38aaf5",
                BungeeCordSpoofModule.forwardedHost("play.example.org", "127.0.0.1", ID, null));
    }

    @Test
    void appendsProfilePropertiesAsBungeeCordJson() {
        String host = BungeeCordSpoofModule.forwardedHost(
                "play.example.org", "127.0.0.1", ID, List.of(new Property("textures", "dGV4dHVyZXM=", "c2lnbmF0dXJl")));

        assertEquals(
                "play.example.org\u0000127.0.0.1\u0000069a79f444e94726a5befca90e38aaf5\u0000"
                        + "[{\"name\":\"textures\",\"value\":\"dGV4dHVyZXM=\",\"signature\":\"c2lnbmF0dXJl\"}]",
                host);
    }

    @Test
    void omitsMissingSignaturesAndAllowsAnEmptyPropertyList() {
        assertEquals(
                "[{\"name\":\"textures\",\"value\":\"abc\"}]",
                BungeeCordSpoofModule.propertiesJson(List.of(new Property("textures", "abc"))));
        assertEquals("[]", BungeeCordSpoofModule.propertiesJson(List.of()));
    }
}
