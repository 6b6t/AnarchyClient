package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.PortalMenuModule;
import net.blockhost.anarchyclient.module.impl.PortalsModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerPortalMixin {

    @Redirect(
            method = "handlePortalTransitionEffect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;isAllowedInPortal()Z")
    )
    private boolean anarchyclient$allowScreensInPortals(final Screen screen) {
        return PortalMenuModule.active() || PortalsModule.active() || screen.isAllowedInPortal();
    }
}
