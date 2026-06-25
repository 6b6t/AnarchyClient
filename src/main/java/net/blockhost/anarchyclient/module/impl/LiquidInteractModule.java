package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LiquidInteractModule extends Module {

    public LiquidInteractModule() {
        super("liquid_interact", "Liquid Interact", ModuleCategory.WORLD);
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || hand != InteractionHand.MAIN_HAND
                || !(player.getMainHandItem().getItem() instanceof BlockItem)) {
            return false;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = client.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK || client.level.getFluidState(hit.getBlockPos()).isEmpty()) {
            return false;
        }
        InteractionResult result = client.gameMode.useItemOn(player, hand, hit);
        if (result.consumesAction()) {
            player.swing(hand);
            return true;
        }
        return false;
    }
}
