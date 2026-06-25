package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class PayloadInspectorModule extends Module {

    private int lastPayloadSequence = -1;

    public PayloadInspectorModule() {
        super("payload_inspector", "Payload Inspector", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.lastPayloadSequence = -1;
    }

    @Override
    public void tick(final Minecraft client) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        if (client.player == null || snapshot.payloadSequence() == this.lastPayloadSequence) {
            return;
        }
        this.lastPayloadSequence = snapshot.payloadSequence();
        String channels = snapshot.payloadChannels().stream()
                .map(Object::toString)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", "));
        if (channels.isBlank()) {
            channels = "none";
        }
        client.player.sendSystemMessage(Component.literal("Payload channels: " + channels + "."));
    }
}
