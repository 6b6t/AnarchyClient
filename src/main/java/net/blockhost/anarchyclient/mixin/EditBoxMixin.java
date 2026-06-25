package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.module.impl.TextFieldProtectModule;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {

    @Shadow
    private String value;

    @Unique
    private String anarchyclient$unmaskedValue;

    @Inject(method = "extractWidgetRenderState", at = @At("HEAD"))
    private void anarchyclient$maskProtectedValue(final GuiGraphicsExtractor graphics, final int mouseX,
                                                  final int mouseY, final float partialTick,
                                                  final CallbackInfo info) {
        if (TextFieldProtectModule.shouldMask(((EditBox) (Object) this).getMessage(), this.value)) {
            this.anarchyclient$unmaskedValue = this.value;
            this.value = TextFieldProtectModule.mask(this.value);
        }
    }

    @Inject(method = "extractWidgetRenderState", at = @At("RETURN"))
    private void anarchyclient$restoreProtectedValue(final GuiGraphicsExtractor graphics, final int mouseX,
                                                     final int mouseY, final float partialTick,
                                                     final CallbackInfo info) {
        if (this.anarchyclient$unmaskedValue != null) {
            this.value = this.anarchyclient$unmaskedValue;
            this.anarchyclient$unmaskedValue = null;
        }
    }
}
