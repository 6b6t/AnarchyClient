package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

public final class AntiAnchorModule extends Module {

    public AntiAnchorModule() {
        super("anti_anchor", "Anti Anchor", ModuleCategory.COMBAT);
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return client.level != null && client.level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof RespawnAnchorBlock;
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return client.player != null && client.player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR);
    }
}
