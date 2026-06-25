package net.blockhost.anarchyclient.server;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

public final class ServerObserver {

    private static final int REQUIRED_TRANSACTION_IDS = 5;
    private static final int MAX_TRANSACTION_IDS = 12;
    private static final int MAX_TIME_SAMPLES = 15;
    private static final Set<String> KNOWN_ANTI_CHEAT_PLUGINS = Set.of(
            "aac",
            "anticheatreloaded",
            "grimac",
            "horizon",
            "intave",
            "kauri",
            "karhu",
            "matrix",
            "negativity",
            "nocheatplus",
            "polar",
            "spartan",
            "themis",
            "verus",
            "vulcan",
            "watchdog"
    );

    private static String serverAddress = "";
    private static String serverName = "";
    private static String rootDomain = "";
    private static ServerType serverType = ServerType.UNKNOWN;
    private static final TreeSet<Identifier> payloadChannels = new TreeSet<>(Comparator.comparing(Identifier::toString));
    private static final ArrayDeque<Integer> transactionIds = new ArrayDeque<>();
    private static boolean capturingTransactions;
    private static String antiCheat = "";
    private static final ArrayDeque<Long> timeIntervalsNanos = new ArrayDeque<>();
    private static long lastTimePacketNanos;
    private static double tps = Double.NaN;
    private static int expectedSuggestionId = -1;
    private static long suggestionDeadlineMillis;
    private static long pluginChatDeadlineMillis;
    private static Set<String> plugins = Set.of();
    private static int pluginScanSequence;
    private static int flagSequence;
    private static FlagInfo lastFlag = FlagInfo.none();
    private static int velocityCorrections;
    private static int tabAdds;
    private static int tabRemoves;
    private static int payloadSequence;

    private ServerObserver() {
    }

    public static void observeTick(final Minecraft client) {
        ServerData server = client == null ? null : client.getCurrentServer();
        if (server == null) {
            return;
        }
        serverAddress = normalizeAddress(server.ip);
        serverName = normalizeNullable(server.name);
        rootDomain = rootDomain(serverAddress);
        long now = System.currentTimeMillis();
        if (expectedSuggestionId >= 0 && now > suggestionDeadlineMillis) {
            expectedSuggestionId = -1;
        }
        if (pluginChatDeadlineMillis > 0 && now > pluginChatDeadlineMillis) {
            pluginChatDeadlineMillis = 0;
        }
    }

    public static void observeJoined(final Minecraft client) {
        reset();
        observeTick(client);
    }

    public static void observeLeft() {
        reset();
    }

    public static void observeReceived(final Minecraft client, final Packet<?> packet) {
        observeTick(client);
        if (packet instanceof ClientboundLoginPacket login) {
            transactionIds.clear();
            capturingTransactions = true;
            serverType = login.onlineMode() ? ServerType.PREMIUM : ServerType.CRACKED;
            return;
        }
        if (packet instanceof ClientboundPingPacket ping && capturingTransactions) {
            rememberTransactionId(ping.getId());
            if (transactionIds.size() >= REQUIRED_TRANSACTION_IDS) {
                capturingTransactions = false;
                antiCheat = guessAntiCheat(serverAddress, List.copyOf(transactionIds)).orElse("Unknown");
            }
            return;
        }
        if (packet instanceof ClientboundCustomPayloadPacket payload) {
            if (payloadChannels.add(payload.payload().type().id())) {
                payloadSequence++;
            }
            return;
        }
        if (packet instanceof ClientboundSetTimePacket) {
            rememberTimePacket(System.nanoTime());
            return;
        }
        if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            if (client.player != null && motion.id() == client.player.getId() && motion.movement().lengthSqr() > 0.0001) {
                velocityCorrections++;
            }
            return;
        }
        if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
            tabAdds += info.newEntries().size();
            return;
        }
        if (packet instanceof ClientboundPlayerInfoRemovePacket remove) {
            tabRemoves += remove.profileIds().size();
            return;
        }
        if (packet instanceof ClientboundCommandSuggestionsPacket suggestions) {
            handleCommandSuggestions(suggestions);
            return;
        }
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            handleSystemChat(systemChat.content());
        }
    }

    public static int beginCommandSuggestionPluginScan(final long timeoutMillis) {
        plugins = Set.of();
        expectedSuggestionId = ThreadLocalRandom.current().nextInt(1, 32768);
        suggestionDeadlineMillis = System.currentTimeMillis() + Math.max(250L, timeoutMillis);
        return expectedSuggestionId;
    }

    public static void beginPluginChatCapture(final long timeoutMillis) {
        plugins = Set.of();
        pluginChatDeadlineMillis = System.currentTimeMillis() + Math.max(250L, timeoutMillis);
    }

    public static boolean pluginScanPending() {
        return expectedSuggestionId >= 0 || pluginChatDeadlineMillis > 0;
    }

    public static void recordFlag(final FlagReason reason, final Vec3 position, final float yaw, final float pitch,
                                  final String detail) {
        flagSequence++;
        lastFlag = new FlagInfo(flagSequence, reason, position, yaw, pitch, normalizeNullable(detail), System.currentTimeMillis());
    }

    public static int flagSequence() {
        return flagSequence;
    }

    public static FlagInfo lastFlag() {
        return lastFlag;
    }

    public static Snapshot snapshot() {
        return new Snapshot(
                serverAddress,
                serverName,
                rootDomain,
                serverType,
                Double.isNaN(tps) ? Optional.empty() : Optional.of(tps),
                List.copyOf(transactionIds),
                antiCheat.isBlank() ? Optional.empty() : Optional.of(antiCheat),
                Set.copyOf(payloadChannels),
                Set.copyOf(plugins),
                pluginScanSequence,
                flagSequence,
                lastFlag,
                velocityCorrections,
                tabAdds,
                tabRemoves,
                payloadSequence
        );
    }

    public static Fingerprint fingerprint() {
        Snapshot snapshot = snapshot();
        return new Fingerprint(
                snapshot.rootDomain(),
                snapshot.antiCheat().orElse(""),
                snapshot.environmentLabels(),
                snapshot.plugins(),
                snapshot.payloadChannels().stream().map(Identifier::toString).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)),
                snapshot.safetyScore()
        );
    }

    public static Set<String> knownAntiCheatPlugins() {
        return KNOWN_ANTI_CHEAT_PLUGINS;
    }

    public static boolean isKnownAntiCheatPlugin(final String plugin) {
        return plugin != null && KNOWN_ANTI_CHEAT_PLUGINS.contains(plugin.toLowerCase(Locale.ROOT));
    }

    public static Optional<String> guessAntiCheat(final String address, final List<Integer> ids) {
        if (ids == null || ids.size() < REQUIRED_TRANSACTION_IDS) {
            return Optional.empty();
        }
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.endsWith("hypixel.net")) {
            return Optional.of("Watchdog");
        }

        List<Integer> diffs = new ArrayList<>();
        for (int i = 1; i < ids.size(); i++) {
            diffs.add(ids.get(i) - ids.get(i - 1));
        }
        int first = ids.getFirst();
        boolean constantDiff = diffs.stream().allMatch(diff -> diff.equals(diffs.getFirst()));
        if (constantDiff && diffs.getFirst() == 1) {
            return Optional.of(switch (rangeCode(first)) {
                case "vulcan" -> "Vulcan";
                case "matrix" -> "Matrix";
                case "grizzly" -> "Grizzly";
                default -> "Verus";
            });
        }
        if (constantDiff && diffs.getFirst() == -1) {
            if (first >= -8287 && first <= -8280) {
                return Optional.of("Errata");
            }
            if (first >= -5 && first <= 0) {
                return Optional.of("Grim");
            }
            if (first >= -3000 && first <= -2995) {
                return Optional.of("Karhu");
            }
            return Optional.of(first < -3000 ? "Intave" : "Polar");
        }
        if (ids.get(0).equals(ids.get(1)) && increasingByOne(ids, 2)) {
            return Optional.of("Verus");
        }
        if (diffs.size() >= 2 && diffs.get(0) >= 100 && diffs.get(1) == -1 && diffs.subList(2, diffs.size()).stream().allMatch(diff -> diff == -1)) {
            return Optional.of("Polar");
        }
        if (first < -3000 && ids.contains(0)) {
            return Optional.of("Intave");
        }
        if (ids.size() >= 3 && ids.get(0) == -30767 && ids.get(1) == -30766 && ids.get(2) == -25767 && increasingByOne(ids, 3)) {
            return Optional.of("Old Vulcan");
        }
        return Optional.of("Unknown");
    }

    public static String rootDomain(final String address) {
        String host = normalizeAddress(address);
        int colon = host.indexOf(':');
        if (colon >= 0) {
            host = host.substring(0, colon);
        }
        String[] parts = host.split("\\.");
        if (parts.length <= 2) {
            return host;
        }
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }

    static Set<String> extractPluginsFromSuggestions(final List<ClientboundCommandSuggestionsPacket.Entry> suggestions) {
        Set<String> names = new LinkedHashSet<>();
        for (ClientboundCommandSuggestionsPacket.Entry entry : suggestions) {
            String text = entry.text();
            if (text == null) {
                continue;
            }
            String normalized = text.trim();
            while (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            int colon = normalized.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String plugin = normalizePluginName(normalized.substring(0, colon));
            if (!plugin.isBlank()) {
                names.add(plugin);
            }
        }
        return Set.copyOf(names);
    }

    static Set<String> extractPluginsFromMessage(final String message) {
        if (message == null) {
            return Set.of();
        }
        String plain = message.strip();
        int pluginsIndex = plain.toLowerCase(Locale.ROOT).indexOf("plugins");
        int separator = plain.indexOf(':', Math.max(0, pluginsIndex));
        if (pluginsIndex < 0 || separator < 0 || separator >= plain.length() - 1) {
            return Set.of();
        }
        Set<String> names = new LinkedHashSet<>();
        for (String token : plain.substring(separator + 1).split(",")) {
            String plugin = normalizePluginName(token);
            if (!plugin.isBlank()) {
                names.add(plugin);
            }
        }
        return Set.copyOf(names);
    }

    private static void rememberTransactionId(final int id) {
        transactionIds.addLast(id);
        while (transactionIds.size() > MAX_TRANSACTION_IDS) {
            transactionIds.removeFirst();
        }
    }

    private static void rememberTimePacket(final long nowNanos) {
        if (lastTimePacketNanos > 0) {
            timeIntervalsNanos.addLast(nowNanos - lastTimePacketNanos);
            while (timeIntervalsNanos.size() > MAX_TIME_SAMPLES) {
                timeIntervalsNanos.removeFirst();
            }
            double averageSeconds = timeIntervalsNanos.stream()
                    .mapToDouble(interval -> interval / 1_000_000_000.0)
                    .average()
                    .orElse(Double.NaN);
            if (!Double.isNaN(averageSeconds) && averageSeconds > 0.0) {
                tps = Math.max(0.0, Math.min(20.0, 20.0 / averageSeconds));
            }
        }
        lastTimePacketNanos = nowNanos;
    }

    private static void handleCommandSuggestions(final ClientboundCommandSuggestionsPacket suggestions) {
        if (suggestions.id() != expectedSuggestionId) {
            return;
        }
        expectedSuggestionId = -1;
        storePlugins(extractPluginsFromSuggestions(suggestions.suggestions()));
    }

    private static void handleSystemChat(final Component component) {
        if (pluginChatDeadlineMillis <= 0 || component == null) {
            return;
        }
        Set<String> parsed = extractPluginsFromMessage(component.getString());
        if (!parsed.isEmpty()) {
            pluginChatDeadlineMillis = 0;
            storePlugins(parsed);
        }
    }

    private static void storePlugins(final Set<String> parsed) {
        plugins = parsed == null ? Set.of() : Set.copyOf(parsed);
        pluginScanSequence++;
    }

    private static boolean increasingByOne(final List<Integer> ids, final int startIndex) {
        for (int i = Math.max(1, startIndex); i < ids.size(); i++) {
            if (ids.get(i) - ids.get(i - 1) != 1) {
                return false;
            }
        }
        return true;
    }

    private static String rangeCode(final int first) {
        if (first >= -23772 && first <= -23762) {
            return "vulcan";
        }
        if ((first >= 95 && first <= 105) || (first >= -20005 && first <= -19995)) {
            return "matrix";
        }
        if (first >= -32773 && first <= -32762) {
            return "grizzly";
        }
        return "";
    }

    private static String normalizeAddress(final String value) {
        return normalizeNullable(value).toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(final String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizePluginName(final String value) {
        StringBuilder builder = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    private static void reset() {
        serverAddress = "";
        serverName = "";
        rootDomain = "";
        serverType = ServerType.UNKNOWN;
        payloadChannels.clear();
        transactionIds.clear();
        capturingTransactions = false;
        antiCheat = "";
        timeIntervalsNanos.clear();
        lastTimePacketNanos = 0L;
        tps = Double.NaN;
        expectedSuggestionId = -1;
        suggestionDeadlineMillis = 0L;
        pluginChatDeadlineMillis = 0L;
        plugins = Set.of();
        pluginScanSequence = 0;
        flagSequence = 0;
        lastFlag = FlagInfo.none();
        velocityCorrections = 0;
        tabAdds = 0;
        tabRemoves = 0;
        payloadSequence = 0;
    }

    public enum ServerType {
        UNKNOWN,
        PREMIUM,
        CRACKED
    }

    public enum FlagReason {
        LAGBACK,
        FORCE_ROTATE,
        INVALID_ATTRIBUTES
    }

    public record FlagInfo(int sequence, FlagReason reason, Vec3 position, float yaw, float pitch, String detail,
                           long createdAtMillis) {

        static FlagInfo none() {
            return new FlagInfo(0, FlagReason.LAGBACK, Vec3.ZERO, 0.0F, 0.0F, "", 0L);
        }
    }

    public record Snapshot(
            String serverAddress,
            String serverName,
            String rootDomain,
            ServerType serverType,
            Optional<Double> tps,
            List<Integer> transactionIds,
            Optional<String> antiCheat,
            Set<Identifier> payloadChannels,
            Set<String> plugins,
            int pluginScanSequence,
            int flagSequence,
            FlagInfo lastFlag,
            int velocityCorrections,
            int tabAdds,
            int tabRemoves,
            int payloadSequence
    ) {
        public Set<String> environmentLabels() {
            Set<String> labels = new LinkedHashSet<>();
            this.antiCheat.ifPresent(value -> {
                String normalized = value.toLowerCase(Locale.ROOT);
                if (normalized.contains("grim")) {
                    labels.add("grim-like");
                } else if (normalized.contains("vulcan")) {
                    labels.add("vulcan-like");
                } else if (normalized.contains("matrix")) {
                    labels.add("matrix-like");
                } else if (normalized.contains("verus")) {
                    labels.add("verus-like");
                } else if (!normalized.equals("unknown")) {
                    labels.add("known-anticheat");
                }
            });
            if (this.flagSequence >= 2 || this.velocityCorrections >= 3) {
                labels.add("strict-movement");
            }
            if (this.lastFlag.sequence() > 0 && this.lastFlag.reason() == FlagReason.FORCE_ROTATE) {
                labels.add("strict-rotation");
            }
            if (!this.plugins.isEmpty()) {
                labels.add("plugin-leak");
            }
            if (this.tps.orElse(20.0) < 16.0) {
                labels.add("low-tps");
            }
            if (this.tabAdds + this.tabRemoves >= 10) {
                labels.add("tab-churn");
            }
            return Set.copyOf(labels);
        }

        public int safetyScore() {
            int score = 0;
            if (this.antiCheat.isPresent() && !"Unknown".equals(this.antiCheat.orElse(""))) {
                score += 25;
            }
            score += Math.min(35, this.flagSequence * 8);
            score += Math.min(20, this.velocityCorrections * 5);
            if (this.environmentLabels().contains("strict-rotation")) {
                score += 15;
            }
            return Math.min(100, score);
        }
    }

    public record Fingerprint(String rootDomain, String antiCheat, Set<String> labels, Set<String> plugins,
                              Set<String> payloadChannels, int safetyScore) {
        public boolean hasLabel(final String label) {
            return this.labels.contains(label);
        }
    }
}
