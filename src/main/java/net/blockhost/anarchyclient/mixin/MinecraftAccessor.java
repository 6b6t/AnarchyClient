package net.blockhost.anarchyclient.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("rightClickDelay")
    int anarchyclient$rightClickDelay();

    @Accessor("rightClickDelay")
    void anarchyclient$setRightClickDelay(int delay);
}
