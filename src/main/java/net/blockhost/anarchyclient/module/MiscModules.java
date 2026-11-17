package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AntiPacketKickModule;
import net.blockhost.anarchyclient.module.impl.AntiVanishModule;
import net.blockhost.anarchyclient.module.impl.AutoDisableModule;
import net.blockhost.anarchyclient.module.impl.AutoGgModule;
import net.blockhost.anarchyclient.module.impl.AutoLoginModule;
import net.blockhost.anarchyclient.module.impl.AutoReconnectModule;
import net.blockhost.anarchyclient.module.impl.BetterChatModule;
import net.blockhost.anarchyclient.module.impl.BookBotModule;
import net.blockhost.anarchyclient.module.impl.BungeeCordSpoofModule;
import net.blockhost.anarchyclient.module.impl.ChatBotModule;
import net.blockhost.anarchyclient.module.impl.ChatSpammerModule;
import net.blockhost.anarchyclient.module.impl.ColorSignsModule;
import net.blockhost.anarchyclient.module.impl.CoordLoggerModule;
import net.blockhost.anarchyclient.module.impl.CustomPacketsModule;
import net.blockhost.anarchyclient.module.impl.DebugRecorderModule;
import net.blockhost.anarchyclient.module.impl.DiscordPresenceModule;
import net.blockhost.anarchyclient.module.impl.FakePlayerModule;
import net.blockhost.anarchyclient.module.impl.GamemodeNotifierModule;
import net.blockhost.anarchyclient.module.impl.InteractionMenuModule;
import net.blockhost.anarchyclient.module.impl.InventoryTrackerModule;
import net.blockhost.anarchyclient.module.impl.ItemGeneratorModule;
import net.blockhost.anarchyclient.module.impl.MacroModule;
import net.blockhost.anarchyclient.module.impl.MessageAuraModule;
import net.blockhost.anarchyclient.module.impl.MiddleClickActionModule;
import net.blockhost.anarchyclient.module.impl.NameProtectModule;
import net.blockhost.anarchyclient.module.impl.NotebotModule;
import net.blockhost.anarchyclient.module.impl.NotifierModule;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;
import net.blockhost.anarchyclient.module.impl.PacketCancellerModule;
import net.blockhost.anarchyclient.module.impl.PacketLoggerModule;
import net.blockhost.anarchyclient.module.impl.ServerSpoofModule;
import net.blockhost.anarchyclient.module.impl.SilentDisconnectModule;
import net.blockhost.anarchyclient.module.impl.SoundBlockerModule;
import net.blockhost.anarchyclient.module.impl.SwarmModule;
import net.blockhost.anarchyclient.module.impl.TextFieldProtectModule;

/**
 * Registers the default utility, chat, and fun modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class MiscModules {

    private MiscModules() {
    }

    static void register(final ModuleManager modules) {
        modules.register(new CoordLoggerModule());
        modules.register(new GamemodeNotifierModule());
        modules.register(new NotifierModule());
        modules.register(new AntiVanishModule());
        modules.register(new SilentDisconnectModule());
        modules.register(new SoundBlockerModule());
        modules.register(new PacketLoggerModule());
        modules.register(new CustomPacketsModule());
        modules.register(new BungeeCordSpoofModule());
        modules.register(new ServerSpoofModule());
        modules.register(new PacketCancellerModule());
        modules.register(new AntiPacketKickModule());
        modules.register(new DebugRecorderModule());
        modules.register(new ColorSignsModule());
        modules.register(new AutoDisableModule(modules));
        modules.register(new MacroModule());
        modules.register(new MiddleClickActionModule());
        modules.register(new InteractionMenuModule());
        modules.register(new AutoLoginModule());
        modules.register(new AutoReconnectModule());
        modules.register(new BetterChatModule());
        modules.register(new NameProtectModule());
        modules.register(new TextFieldProtectModule());
        modules.register(new InventoryTrackerModule());
        modules.register(new AutoGgModule());
        modules.register(new ChatBotModule());
        modules.register(new ChatSpammerModule());
        modules.register(new MessageAuraModule());
        modules.register(new BookBotModule());
        modules.register(new NotebotModule());
        modules.register(new DiscordPresenceModule());
        modules.register(new FakePlayerModule());
        modules.register(new SwarmModule());
        modules.register(new ItemGeneratorModule());
        modules.register(new NyanCatGifSpammerModule());
    }
}
