package net.blockhost.anarchyclient.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.blockhost.anarchyclient.AnarchyClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    /** Records module bind presses before vanilla handles the key, which may open a screen with it. */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void anarchyclient$recordKeyPress(
            final long handle, final int action, final KeyEvent event, final CallbackInfo info) {
        Minecraft client = Minecraft.getInstance();
        if (action == InputConstants.PRESS && handle == client.getWindow().handle()) {
            AnarchyClient.MODULES.recordKeyPress(event.key(), client.gui.screen() == null);
        }
    }
}
