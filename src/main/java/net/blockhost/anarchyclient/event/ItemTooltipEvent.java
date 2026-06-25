package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemTooltipEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private final ItemStack stack;
    private final List<Component> lines;

    public ItemTooltipEvent(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        this.client = client;
        this.stack = stack;
        this.lines = lines;
    }

    public Minecraft client() {
        return this.client;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public List<Component> lines() {
        return this.lines;
    }
}
