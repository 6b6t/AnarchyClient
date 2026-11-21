package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@ExtendWith(MinecraftBootstrapExtension.class)
class MinecraftPacketCompatibilityTest {

    @ParameterizedTest
    @EnumSource(SignTextSlot.class)
    void preservesTheSignSideAndPositionWhenAddingColors(final SignTextSlot side) {
        BlockPos pos = new BlockPos(10, 64, -20);
        var packet = new ServerboundSignUpdatePacket(pos, List.of("&aGreen", "plain", "", "&lBold"), side);

        var rewritten = assertInstanceOf(
                ServerboundSignUpdatePacket.class, new ColorSignsModule().replaceSendPacket(null, null, packet));

        assertEquals(pos, rewritten.pos());
        assertEquals(side, rewritten.slot());
        assertEquals(List.of("\u00a7aGreen", "plain", "", "\u00a7lBold"), rewritten.lines());
        assertEquals("&aGreen", packet.lines().getFirst());
    }

    @Test
    void keepsUncoloredSignPackets() {
        var packet =
                new ServerboundSignUpdatePacket(BlockPos.ZERO, List.of("plain", "text", "", ""), SignTextSlot.BACK);
        assertSame(packet, new ColorSignsModule().replaceSendPacket(null, null, packet));
    }

    @Test
    void blocksMissedSwingsWithoutBlockingAttacks() {
        assertTrue(NoSwingModule.shouldCancel(ServerboundPunchPacket.INSTANCE, true));
        assertFalse(NoSwingModule.shouldCancel(ServerboundPunchPacket.INSTANCE, false));
        assertFalse(NoSwingModule.shouldCancel(new ServerboundAttackPacket(42), true));
    }
}
