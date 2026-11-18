package net.blockhost.anarchyclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {

    /** Sends the selected hotbar slot to the server if it changed since the last sync. */
    @Invoker("ensureHasSentCarriedItem")
    void anarchyclient$ensureHasSentCarriedItem();
}
