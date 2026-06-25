package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class NoMiningTraceModule extends Module {

    private static boolean active;

    public NoMiningTraceModule() {
        super("no_mining_trace", "No Mining Trace", ModuleCategory.PLAYER);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static HitResult replacementHitResult(final Minecraft client, final HitResult current) {
        LocalPlayer player = client.player;
        if (!active || player == null || client.level == null || !(current instanceof EntityHitResult)) {
            return current;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult blockHit = client.level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return blockHit.getType() == HitResult.Type.BLOCK ? blockHit : current;
    }
}
