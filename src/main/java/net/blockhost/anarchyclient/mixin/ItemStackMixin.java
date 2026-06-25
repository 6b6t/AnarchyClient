package net.blockhost.anarchyclient.mixin;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.event.ItemTooltipEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true)
    private void anarchyclient$itemTooltip(final Item.TooltipContext context, final @Nullable Player player,
                                           final TooltipFlag tooltipFlag,
                                           final CallbackInfoReturnable<List<Component>> info) {
        List<Component> lines = new ArrayList<>(info.getReturnValue());
        AnarchyClient.MODULES.call(new ItemTooltipEvent(Minecraft.getInstance(), (ItemStack) (Object) this, lines));
        info.setReturnValue(lines);
    }
}
