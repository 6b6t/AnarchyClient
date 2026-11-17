package net.blockhost.anarchyclient.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class AnarchyClientCommands {

    private AnarchyClientCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
    }

    static void register(final CommandDispatcher<FabricClientCommandSource> dispatcher) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = literal("anarchyclient");
        ModuleCommands.register(root);
        PlayerCommands.register(root);
        ChatCommands.register(root);
        ServerCommands.register(root);
        ProfileCommands.register(root);
        WaypointCommands.register(root);
        ItemCommands.register(root);
        WorldCommands.register(root);
        dispatcher.register(root);
        dispatcher.register(literal("ac").redirect(dispatcher.getRoot().getChild("anarchyclient")));
    }
}
