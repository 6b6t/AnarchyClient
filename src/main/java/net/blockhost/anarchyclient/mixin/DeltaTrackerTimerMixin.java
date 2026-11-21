package net.blockhost.anarchyclient.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {

    // Adjust the provider's result, not its argument: Minecraft.getTickTargetMillis clamps the
    // argument to at least the level tick rate, which would cancel any speed-up.
    @Redirect(
            method = "advanceGameTime",
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
    private float anarchyclient$applyTimerMultiplier(final FloatUnaryOperator provider, final float msPerTick) {
        return TimerManager.adjustMspt(provider.apply(msPerTick));
    }
}
