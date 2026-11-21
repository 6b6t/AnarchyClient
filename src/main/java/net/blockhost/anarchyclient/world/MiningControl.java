package net.blockhost.anarchyclient.world;

import net.blockhost.anarchyclient.util.TickClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;

/**
 * Lets a module mine a block over several ticks.
 *
 * <p>Vanilla {@code Minecraft.continueAttack} aborts any block break every tick the attack key is
 * up. While a module renews its claim each tick through {@link #mine}, the mixin skips vanilla's
 * {@code continueAttack}, so the module's progress survives. When the module stops calling
 * {@link #mine}, vanilla takes over on the next tick and aborts the break as usual.
 */
public final class MiningControl {

    private static final TickClaim CLAIM = new TickClaim();

    private MiningControl() {}

    /**
     * Starts or continues breaking {@code pos} and claims mining control for this tick. Call it
     * once per tick for the same block until it breaks. Returns whether a break packet was sent
     * or progress was made; false while another module holds the claim.
     */
    public static boolean mine(final Minecraft client, final String owner, final BlockPos pos, final Direction face) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return false;
        }
        if (client.level.getBlockState(pos).isAir()) {
            release(client, owner);
            return false;
        }
        // Another module is mining: breaking a second block would abort its progress every tick.
        if (!CLAIM.tryClaim(owner)) {
            return false;
        }
        if (!client.gameMode.continueDestroyBlock(pos, face)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, player.getMainHandItem().getAttackAnimation(), false);
        player.connection.send(ServerboundPunchPacket.INSTANCE);
        return true;
    }

    /** Whether {@code owner} may mine now, so a refused module does not switch tools first. */
    public static boolean canMine(final String owner) {
        return CLAIM.canClaim(owner);
    }

    /** Releases the claim and aborts the break in progress if {@code owner} held it. */
    public static void release(final Minecraft client, final String owner) {
        if (CLAIM.release(owner) && client.gameMode != null && client.gameMode.isDestroying()) {
            client.gameMode.stopDestroyBlock();
        }
    }

    /** Returns whether {@code owner} currently controls mining. */
    public static boolean isMining(final String owner) {
        return CLAIM.isHeldBy(owner);
    }

    /**
     * Called once per tick from {@code Minecraft.continueAttack}. Returns whether a module
     * renewed its claim since the last call, in which case vanilla must not touch the break.
     */
    public static boolean suppressVanillaAttack() {
        return CLAIM.consume();
    }
}
