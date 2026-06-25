package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginScannerModule extends Module {

    private final BooleanSetting slash = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("slash")
            .name("/plugins")
            .defaultValue(true)
            .build()));
    private final BooleanSetting bukkit = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("bukkit")
            .name("bukkit")
            .defaultValue(true)
            .build()));
    private final BooleanSetting suggestions = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("suggestions")
            .name("Suggest")
            .defaultValue(true)
            .build()));
    private final NumberSetting timeout = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("timeout")
            .name("Timeout")
            .defaultValue(5.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private ScanStage stage = ScanStage.IDLE;
    private long deadlineMillis;
    private int observedPluginSequence;

    public PluginScannerModule() {
        super("plugin_scanner", "Plugin Scanner", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.stage = ScanStage.START;
        this.deadlineMillis = 0L;
        this.observedPluginSequence = ServerObserver.snapshot().pluginScanSequence();
    }

    @Override
    protected void onDisable() {
        this.stage = ScanStage.IDLE;
        this.deadlineMillis = 0L;
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            this.notify(client, "Plugin Scanner needs a multiplayer connection.");
            this.enabled(false);
            return;
        }
        if (this.hasNewResult()) {
            this.finish(client, ServerObserver.snapshot().plugins());
            return;
        }
        if (this.stage.waiting && System.currentTimeMillis() < this.deadlineMillis) {
            return;
        }
        switch (this.stage) {
            case START -> this.next(client, this.slash.value() ? ScanStage.SLASH : ScanStage.BUKKIT);
            case SLASH -> {
                ChatActions.send(client, "/plugins");
                ServerObserver.beginPluginChatCapture(this.timeoutMillis());
                this.waitFor(ScanStage.SLASH_WAIT);
            }
            case SLASH_WAIT -> this.next(client, this.bukkit.value() ? ScanStage.BUKKIT : ScanStage.SUGGESTIONS);
            case BUKKIT -> {
                ChatActions.send(client, "/bukkit:plugins");
                ServerObserver.beginPluginChatCapture(this.timeoutMillis());
                this.waitFor(ScanStage.BUKKIT_WAIT);
            }
            case BUKKIT_WAIT -> this.next(client, ScanStage.SUGGESTIONS);
            case SUGGESTIONS -> {
                if (!this.suggestions.value()) {
                    this.finish(client, Set.of());
                    return;
                }
                int id = ServerObserver.beginCommandSuggestionPluginScan(this.timeoutMillis());
                client.getConnection().send(new ServerboundCommandSuggestionPacket(id, "/"));
                this.waitFor(ScanStage.SUGGESTIONS_WAIT);
            }
            case SUGGESTIONS_WAIT -> this.finish(client, ServerObserver.snapshot().plugins());
            case IDLE -> {
            }
        }
    }

    private void next(final Minecraft client, final ScanStage next) {
        this.stage = next;
        this.tick(client);
    }

    private void waitFor(final ScanStage waitingStage) {
        this.stage = waitingStage;
        this.deadlineMillis = System.currentTimeMillis() + this.timeoutMillis();
    }

    private boolean hasNewResult() {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        return snapshot.pluginScanSequence() != this.observedPluginSequence && !snapshot.plugins().isEmpty();
    }

    private void finish(final Minecraft client, final Set<String> plugins) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        if (plugins == null || plugins.isEmpty()) {
            this.notify(client, "No plugins found.");
        } else {
            String list = plugins.stream()
                    .sorted(Comparator.naturalOrder())
                    .map(plugin -> ServerObserver.isKnownAntiCheatPlugin(plugin) ? plugin + " (AC)" : plugin)
                    .collect(Collectors.joining(", "));
            this.notify(client, "Plugins: " + list + ".");
            ServerProfileStore.recordPlugins(snapshot.rootDomain(), plugins);
        }
        this.enabled(false);
    }

    private void notify(final Minecraft client, final String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    private long timeoutMillis() {
        return Math.max(250L, this.timeout.value().longValue() * 1000L);
    }

    private enum ScanStage {
        IDLE(false),
        START(false),
        SLASH(false),
        SLASH_WAIT(true),
        BUKKIT(false),
        BUKKIT_WAIT(true),
        SUGGESTIONS(false),
        SUGGESTIONS_WAIT(true);

        private final boolean waiting;

        ScanStage(final boolean waiting) {
            this.waiting = waiting;
        }
    }
}
