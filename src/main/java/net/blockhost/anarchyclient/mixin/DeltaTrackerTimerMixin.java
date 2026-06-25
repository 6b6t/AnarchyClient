package net.blockhost.anarchyclient.mixin;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DeltaTracker.Timer.class)
public abstract class DeltaTrackerTimerMixin {

    @ModifyArg(
            method = "advanceGameTime",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"
            ),
            index = 0
    )
    private float anarchyclient$applyTimerMultiplier(final float msPerTick) {
        return TimerManager.adjustMspt(msPerTick);
    }
}
