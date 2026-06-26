package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;

public final class AntiAnchorModule extends Module {

    private final BooleanSetting placeSlab = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("place_slab")
            .name("Place Slab")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AntiAnchorModule() {
        super("anti_anchor", "Anti Anchor", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.placeSlab.value() || client.player == null || client.level == null) {
            return;
        }
        BlockPos feet = client.player.blockPosition();
        BlockPos middle = feet.above();
        BlockPos anchor = feet.above(2);
        if (client.level.getBlockState(anchor).getBlock() instanceof RespawnAnchorBlock
                && client.level.getBlockState(middle).canBeReplaced()) {
            BlockPlacement.place(client, this, middle, this.rotate.value(), 60.0F,
                    stack -> Block.byItem(stack.getItem()) instanceof SlabBlock);
        }
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
