package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.AntiQuitModule;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftDisconnectMixin {

    // Only the pause menu's Disconnect and Save and Quit buttons call this method; kicks and
    // connection losses take other paths, so Anti Quit never holds those back.
    @Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
    private void anarchyclient$confirmDisconnect(final Component message, final CallbackInfo info) {
        if (AntiQuitModule.interceptDisconnect((Minecraft) (Object) this)) {
            info.cancel();
        }
    }
}
