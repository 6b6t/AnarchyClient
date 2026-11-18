package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    private boolean placingSlab;

    public AntiAnchorModule() {
        super("anti_anchor", "Anti Anchor", ModuleCategory.COMBAT,
                "While you crouch, places a slab between you and a respawn anchor above your head, and blocks you from using anchors yourself.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.placeSlab.value() || client.player == null || client.level == null) {
            return;
        }
        BlockPos feet = client.player.blockPosition();
        BlockPos middle = feet.above();
        BlockPos anchor = feet.above(2);
        if (!(client.level.getBlockState(anchor).getBlock() instanceof RespawnAnchorBlock)
                || !client.level.getBlockState(middle).canBeReplaced()) {
            return;
        }
        // Only a top slab (upper half of the head cell) fits, and only above a crouching player (1.5 blocks tall).
        // Clicking the anchor's underside places exactly that.
        if (!client.player.isCrouching()) {
            this.debugValue("status", "crouch to place the slab");
            return;
        }
        this.placingSlab = true;
        try {
            WorldInteraction.useOnBlock(client, this, anchor, Direction.DOWN,
                    stack -> Block.byItem(stack.getItem()) instanceof SlabBlock, this.rotate.value());
        } finally {
            this.placingSlab = false;
        }
    }

    @Override
    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return !this.placingSlab && client.level != null
                && client.level.getBlockState(hitResult.getBlockPos()).getBlock() instanceof RespawnAnchorBlock;
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return client.player != null && client.player.getItemInHand(hand).is(Items.RESPAWN_ANCHOR);
    }
}
