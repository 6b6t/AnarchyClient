package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SwarmModule extends Module {

    private final SelectSetting role = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("role")
            .name("Role")
            .defaultValue("Worker")
            .addAllOptions(List.of("Host", "Worker"))
            .build()));
    private final StringSetting channel = this.setting(StringSetting.from(StringSetting.builder()
            .id("channel")
            .name("Token")
            .defaultValue("local")
            .build()));
    private final StringSetting host = this.setting(StringSetting.from(StringSetting.builder()
            .id("host")
            .name("Host")
            .defaultValue("127.0.0.1")
            .build()));
    private final NumberSetting port = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("port")
            .name("Port")
            .defaultValue(51234.0)
            .min(1024.0)
            .max(65535.0)
            .step(1.0)
            .build()));
    private final BooleanSetting allowCommands = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_commands")
            .name("Commands")
            .defaultValue(false)
            .build()));
    private final Queue<String> pendingActions = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<PeerConnection> peers = new CopyOnWriteArrayList<>();
    private volatile ServerSocket serverSocket;
    private volatile Socket workerSocket;
    private volatile boolean running;
    private Thread serviceThread;
    private String lastStatus = "stopped";

    public SwarmModule() {
        super("swarm", "Swarm", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.running) {
            this.startService();
        }
        String action;
        while ((action = this.pendingActions.poll()) != null) {
            if (action.startsWith("/") && !this.allowCommands.value()) {
                this.lastStatus = "blocked command";
                continue;
            }
            ChatActions.send(client, action);
            this.lastStatus = "ran " + preview(action);
        }
        this.debugValue("status", this.status());
        this.debugValue("peers", this.peers.size());
    }

    @Override
    protected void onEnable() {
        this.startService();
    }

    @Override
    protected void onDisable() {
        this.stopService();
        this.pendingActions.clear();
        this.clearDebugValues();
    }

    public String status() {
        return this.role.value().toLowerCase(Locale.ROOT) + " " + this.host.value() + ":" + this.port.value().intValue()
                + " peers=" + this.peers.size() + " " + this.lastStatus;
    }

    public int broadcast(final String action) {
        if (!"Host".equals(this.role.value())) {
            return 0;
        }
        String line = encodeRun(action);
        int sent = 0;
        for (PeerConnection peer : this.peers) {
            if (peer.send(line)) {
                sent++;
            }
        }
        this.lastStatus = "sent to " + sent;
        return sent;
    }

    private synchronized void startService() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.lastStatus = "starting";
        if ("Host".equals(this.role.value())) {
            this.serviceThread = Thread.ofVirtual().name("AnarchyClient Swarm Host").start(this::runHost);
        } else {
            this.serviceThread = Thread.ofVirtual().name("AnarchyClient Swarm Worker").start(this::runWorker);
        }
    }

    private synchronized void stopService() {
        this.running = false;
        closeQuietly(this.serverSocket);
        closeQuietly(this.workerSocket);
        for (PeerConnection peer : this.peers) {
            peer.close();
        }
        this.peers.clear();
        this.serverSocket = null;
        this.workerSocket = null;
        this.serviceThread = null;
        this.lastStatus = "stopped";
    }

    private void runHost() {
        try (ServerSocket server = new ServerSocket(this.port.value().intValue())) {
            this.serverSocket = server;
            this.lastStatus = "listening";
            while (this.running) {
                Socket socket = server.accept();
                Thread.ofVirtual().name("AnarchyClient Swarm Peer").start(() -> this.handlePeer(socket));
            }
        } catch (IOException exception) {
            if (this.running) {
                this.lastStatus = "host error " + exception.getClass().getSimpleName();
            }
        } finally {
            this.running = false;
        }
    }

    private void handlePeer(final Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            String hello = reader.readLine();
            if (hello == null || !hello.equals("HELLO\t" + this.channel.value())) {
                socket.close();
                return;
            }
            PeerConnection peer = new PeerConnection(socket, writer);
            this.peers.add(peer);
            this.lastStatus = "peer joined";
            while (this.running && reader.readLine() != null) {
                // Keep the connection open. Host-to-worker is the only command direction for now.
            }
        } catch (IOException ignored) {
        } finally {
            this.peers.removeIf(peer -> peer.socket() == socket);
            closeQuietly(socket);
        }
    }

    private void runWorker() {
        while (this.running) {
            try (Socket socket = new Socket(this.host.value(), this.port.value().intValue())) {
                this.workerSocket = socket;
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer.println("HELLO\t" + this.channel.value());
                this.lastStatus = "connected";
                String line;
                while (this.running && (line = reader.readLine()) != null) {
                    decodeRun(line).ifPresent(this.pendingActions::add);
                }
            } catch (IOException exception) {
                if (this.running) {
                    this.lastStatus = "retrying";
                    sleep(2000L);
                }
            }
        }
    }

    private String encodeRun(final String action) {
        String encoded = Base64.getEncoder().encodeToString(action.getBytes(StandardCharsets.UTF_8));
        return "RUN\t" + this.channel.value() + "\t" + encoded;
    }

    private java.util.Optional<String> decodeRun(final String line) {
        String[] parts = line.split("\\t", 3);
        if (parts.length != 3 || !"RUN".equals(parts[0]) || !this.channel.value().equals(parts[1])) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private static void closeQuietly(final AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String preview(final String action) {
        return action.length() <= 24 ? action : action.substring(0, 24);
    }

    private record PeerConnection(Socket socket, PrintWriter writer) {

        private boolean send(final String line) {
            this.writer.println(line);
            return !this.writer.checkError();
        }

        private void close() {
            closeQuietly(this.socket);
        }
    }
}
