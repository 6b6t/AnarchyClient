package net.blockhost.anarchyclient.module.impl;

import com.google.gson.JsonObject;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

import java.io.Closeable;
import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DiscordPresenceModule extends Module {

    private final StringSetting applicationId = this.setting(StringSetting.from(StringSetting.builder()
            .id("application_id")
            .name("App ID")
            .defaultValue("")
            .build()));
    private final StringSetting status = this.setting(StringSetting.from(StringSetting.builder()
            .id("status")
            .name("Details")
            .defaultValue("Playing AnarchyClient")
            .build()));
    private final StringSetting state = this.setting(StringSetting.from(StringSetting.builder()
            .id("state")
            .name("State")
            .defaultValue("{server}")
            .build()));
    private DiscordIpcClient ipcClient;
    private long startEpochSeconds;
    private int reconnectCooldownTicks;
    private int updateCooldownTicks;
    private String lastPayload = "";

    public DiscordPresenceModule() {
        super("discord_presence", "Discord Presence", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.applicationId.value().isBlank()) {
            this.debugValue("status", "missing app id");
            return;
        }
        if (this.ipcClient == null && this.reconnectCooldownTicks-- <= 0) {
            this.connect();
        }
        if (this.ipcClient == null || this.updateCooldownTicks-- > 0) {
            return;
        }
        this.updatePresence(client);
        this.updateCooldownTicks = 200;
    }

    @Override
    protected void onEnable() {
        this.startEpochSeconds = Instant.now().getEpochSecond();
        this.reconnectCooldownTicks = 0;
        this.updateCooldownTicks = 0;
    }

    @Override
    protected void onDisable() {
        this.closeIpc();
        this.clearDebugValues();
    }

    String status() {
        return this.status.value();
    }

    private void connect() {
        try {
            this.ipcClient = DiscordIpcClient.connect(this.applicationId.value().trim());
            this.debugValue("status", "connected");
        } catch (IOException | UnsupportedOperationException exception) {
            this.ipcClient = null;
            this.reconnectCooldownTicks = 200;
            this.debugValue("status", "disconnected");
            this.debugValue("error", exception.getClass().getSimpleName());
        }
    }

    private void updatePresence(final Minecraft client) {
        String payload = this.status.value() + "\n" + this.renderState(client);
        if (payload.equals(this.lastPayload)) {
            return;
        }
        try {
            this.ipcClient.setActivity(this.status.value(), this.renderState(client), this.startEpochSeconds);
            this.lastPayload = payload;
            this.debugValue("status", "updated");
        } catch (IOException exception) {
            this.closeIpc();
            this.reconnectCooldownTicks = 40;
            this.debugValue("status", "lost connection");
        }
    }

    private String renderState(final Minecraft client) {
        String server = client.getCurrentServer() == null ? "Singleplayer" : client.getCurrentServer().ip;
        String player = client.player == null ? "Player" : client.player.getName().getString();
        return this.state.value()
                .replace("{server}", server)
                .replace("{player}", player);
    }

    private void closeIpc() {
        if (this.ipcClient != null) {
            try {
                this.ipcClient.close();
            } catch (IOException ignored) {
            }
        }
        this.ipcClient = null;
        this.lastPayload = "";
    }

    private static final class DiscordIpcClient implements Closeable {

        private static final int OP_HANDSHAKE = 0;
        private static final int OP_FRAME = 1;
        private static final int OP_CLOSE = 2;

        private final SocketChannel channel;

        private DiscordIpcClient(final SocketChannel channel) {
            this.channel = channel;
        }

        private static DiscordIpcClient connect(final String applicationId) throws IOException {
            IOException lastException = null;
            ProtocolFamily family = StandardProtocolFamily.UNIX;
            for (Path path : ipcPaths()) {
                if (!Files.exists(path)) {
                    continue;
                }
                try {
                    SocketChannel channel = SocketChannel.open(family);
                    channel.connect(UnixDomainSocketAddress.of(path));
                    DiscordIpcClient client = new DiscordIpcClient(channel);
                    JsonObject handshake = new JsonObject();
                    handshake.addProperty("v", 1);
                    handshake.addProperty("client_id", applicationId);
                    client.write(OP_HANDSHAKE, handshake);
                    return client;
                } catch (IOException exception) {
                    lastException = exception;
                }
            }
            if (lastException != null) {
                throw lastException;
            }
            throw new IOException("Discord IPC socket not found");
        }

        private void setActivity(final String details, final String state, final long startEpochSeconds) throws IOException {
            JsonObject activity = new JsonObject();
            activity.addProperty("details", details);
            if (!state.isBlank()) {
                activity.addProperty("state", state);
            }
            JsonObject timestamps = new JsonObject();
            timestamps.addProperty("start", startEpochSeconds);
            activity.add("timestamps", timestamps);
            JsonObject assets = new JsonObject();
            assets.addProperty("large_text", "AnarchyClient");
            activity.add("assets", assets);

            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", activity);

            JsonObject frame = new JsonObject();
            frame.addProperty("cmd", "SET_ACTIVITY");
            frame.add("args", args);
            frame.addProperty("nonce", UUID.randomUUID().toString());
            this.write(OP_FRAME, frame);
        }

        private synchronized void write(final int op, final JsonObject payload) throws IOException {
            byte[] data = payload.toString().getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(op);
            header.putInt(data.length);
            header.flip();
            this.channel.write(new ByteBuffer[]{header, ByteBuffer.wrap(data)});
        }

        @Override
        public void close() throws IOException {
            if (this.channel.isOpen()) {
                this.write(OP_CLOSE, new JsonObject());
            }
            this.channel.close();
        }

        private static List<Path> ipcPaths() {
            List<Path> bases = new ArrayList<>();
            addBase(bases, System.getenv("XDG_RUNTIME_DIR"));
            addBase(bases, System.getenv("TMPDIR"));
            addBase(bases, "/tmp");
            List<Path> paths = new ArrayList<>();
            for (Path base : bases) {
                for (int i = 0; i < 10; i++) {
                    paths.add(base.resolve("discord-ipc-" + i));
                }
            }
            return paths;
        }

        private static void addBase(final List<Path> bases, final String value) {
            if (value != null && !value.isBlank()) {
                Path path = Path.of(value);
                if (!bases.contains(path)) {
                    bases.add(path);
                }
            }
        }
    }
}
