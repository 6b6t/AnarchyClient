package net.blockhost.anarchyclient.inventory;

import net.blockhost.anarchyclient.util.TickClaim;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * Holds an item use (eating, drinking, drawing a bow) for a module.
 *
 * <p>Vanilla {@code Minecraft.handleKeybinds} releases the used item on every tick the use key is
 * up, so an item started with {@code useItem} alone stops after one tick. While a module renews its
 * claim each tick through {@link #use}, the mixin skips that release. When the module stops calling
 * {@link #use} or calls {@link #release}, vanilla releases the item as usual.
 */
public final class ItemUseControl {

    private static final TickClaim CLAIM = new TickClaim();

    private ItemUseControl() {}

    /**
     * Starts using the item in {@code hand} if the player is not using one yet, and keeps the use
     * going through the next tick. Call it every tick until the use is done. Returns whether the
     * player is using an item afterwards.
     */
    public static boolean use(final Minecraft client, final String owner, final InteractionHand hand) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return false;
        }
        if (player.isUsingItem()) {
            CLAIM.claim(owner);
            return true;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return false;
        }
        if (client.gameMode.useItem(player, hand) instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.PREDICTED) {
                player.swing(hand, stack.getInteractAnimation(), false);
            }
            player.itemUsed(hand);
        }
        if (!player.isUsingItem()) {
            return false;
        }
        CLAIM.claim(owner);
        return true;
    }

    /**
     * Ends the hold. If {@code owner} held it, the player is still using an item, and the use key
     * is up, the item is released now (a bow fires, food stays uneaten).
     */
    public static void release(final Minecraft client, final String owner) {
        LocalPlayer player = client.player;
        if (!CLAIM.release(owner) || player == null || client.gameMode == null) {
            return;
        }
        if (player.isUsingItem() && !client.options.keyUse.isDown()) {
            client.gameMode.releaseUsingItem(player);
        }
    }

    /** Returns whether {@code owner} currently holds an item use. */
    public static boolean isHolding(final String owner) {
        return CLAIM.isHeldBy(owner);
    }

    /**
     * Called once per tick from {@code Minecraft.handleKeybinds}. Returns whether a module renewed
     * its hold since the last call, in which case vanilla must not release the item.
     */
    public static boolean suppressVanillaRelease() {
        return CLAIM.consume();
    }
}
