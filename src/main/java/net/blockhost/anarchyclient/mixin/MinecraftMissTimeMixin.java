package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.NoMissCooldownModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftMissTimeMixin {

    /**
     * Vanilla sets {@code missTime = 10} after a missed attack only when {@code hasMissTime()} is true.
     */
    @Redirect(
            method = "startAttack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasMissTime()Z")
    )
    private boolean anarchyclient$noMissCooldown(final MultiPlayerGameMode gameMode) {
        return !NoMissCooldownModule.removesMissCooldown() && gameMode.hasMissTime();
    }
}
