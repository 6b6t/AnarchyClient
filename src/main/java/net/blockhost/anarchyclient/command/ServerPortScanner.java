package net.blockhost.anarchyclient.command;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class ServerPortScanner {

    static final int MAX_RANGE_SIZE = 512;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(250);

    private ServerPortScanner() {
    }

    static List<PortCheck> knownPorts() {
        Map<Integer, String> ports = new LinkedHashMap<>();
        ports.put(25565, "Minecraft");
        ports.put(25566, "Velocity/Bungee");
        ports.put(25567, "Proxy alternate");
        ports.put(19132, "Bedrock");
        ports.put(8123, "Dynmap");
        ports.put(8080, "HTTP alternate");
        ports.put(80, "HTTP");
        ports.put(443, "HTTPS");
        ports.put(22, "SSH");
        ports.put(3306, "MySQL");
        ports.put(6379, "Redis");
        List<PortCheck> checks = new ArrayList<>();
        ports.forEach((port, label) -> checks.add(new PortCheck(port, label)));
        return List.copyOf(checks);
    }

    static PortRange range(final int first, final int second) {
        int min = Math.min(first, second);
        int max = Math.max(first, second);
        int count = max - min + 1;
        if (count > MAX_RANGE_SIZE) {
            throw new IllegalArgumentException("Port range is too large: " + count + " ports, max " + MAX_RANGE_SIZE + ".");
        }
        return new PortRange(min, max);
    }

    static List<PortResult> scan(final String host, final List<PortCheck> checks) {
        List<PortResult> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PortResult>> futures = checks.stream()
                    .map(check -> executor.submit(() -> new PortResult(
                            check.port(),
                            check.label(),
                            open(host, check.port(), DEFAULT_TIMEOUT)
                    )))
                    .toList();
            for (int index = 0; index < futures.size(); index++) {
                Future<PortResult> future = futures.get(index);
                try {
                    results.add(future.get());
                } catch (ExecutionException exception) {
                    PortCheck check = checks.get(index);
                    results.add(new PortResult(check.port(), check.label(), false));
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        return List.copyOf(results);
    }

    static String format(final List<PortResult> results) {
        List<String> open = results.stream()
                .filter(PortResult::open)
                .map(result -> result.port() + " (" + result.label() + ")")
                .toList();
        if (open.isEmpty()) {
            return "No open ports found.";
        }
        return "Open ports: " + String.join(", ", open);
    }

    private static boolean open(final String host, final int port, final Duration timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    record PortCheck(int port, String label) {
    }

    record PortResult(int port, String label, boolean open) {
    }

    record PortRange(int min, int max) {

        List<PortCheck> checks() {
            List<PortCheck> checks = new ArrayList<>();
            for (int port = this.min; port <= this.max; port++) {
                checks.add(new PortCheck(port, "custom"));
            }
            return List.copyOf(checks);
        }
    }
}
