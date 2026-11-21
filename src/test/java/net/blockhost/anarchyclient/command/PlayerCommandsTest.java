package net.blockhost.anarchyclient.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import net.blockhost.anarchyclient.inventory.InventoryTracker;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class PlayerCommandsTest {

    @Test
    void centerModesSnapCoordinatesPredictably() {
        assertEquals(12.5, PlayerCommands.CenterMode.MIDDLE.coordinate(12.2));
        assertEquals(-4.5, PlayerCommands.CenterMode.MIDDLE.coordinate(-4.8));
        assertEquals(12.0, PlayerCommands.CenterMode.CORNER.coordinate(12.2));
        assertEquals(-5.0, PlayerCommands.CenterMode.CORNER.coordinate(-4.8));
    }

    @Test
    void extractsSkinUrlFromMojangProfilePayload() {
        String texturePayload =
                "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/example\"}}}";
        String encoded = Base64.getEncoder().encodeToString(texturePayload.getBytes(StandardCharsets.UTF_8));
        String profile = "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + encoded + "\"}]}";

        assertEquals("https://textures.minecraft.net/texture/example", PlayerCommands.skinUrl(profile));
    }

    @Test
    void describesTrackedInventorySnapshot() {
        InventoryTracker.Snapshot snapshot = new InventoryTracker.Snapshot(
                UUID.randomUUID(),
                "Steve",
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.TOTEM_OF_UNDYING),
                List.of(
                        new ItemStack(Items.DIAMOND_HELMET),
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemStack(Items.DIAMOND_BOOTS)),
                18.5F,
                4.0F,
                12.4,
                100,
                10_000L);

        assertEquals(
                "Steve (18.5 HP +4.0, 12m, 5s ago): main Diamond Sword, off Totem of Undying, "
                        + "armor Diamond Helmet / - / - / Diamond Boots",
                PlayerCommands.describeSnapshot(snapshot, 15_000L));
    }
}
