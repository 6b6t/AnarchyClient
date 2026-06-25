package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.impl.BetterTabModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$tabName(final PlayerInfo info, final CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(AnarchyClient.MODULES.tabPlayerName(Minecraft.getInstance(), info, cir.getReturnValue()));
    }

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$rewritePlayerInfos(final CallbackInfoReturnable<List<PlayerInfo>> cir) {
        cir.setReturnValue(BetterTabModule.rewritePlayerInfos(cir.getReturnValue()));
    }
}
