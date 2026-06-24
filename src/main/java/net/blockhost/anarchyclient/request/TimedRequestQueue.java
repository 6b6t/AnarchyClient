package net.blockhost.anarchyclient.request;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TimedRequestQueue<T> {

    private final List<Entry<T>> entries = new ArrayList<>();
    private long sequence;
    private int tick;

    public void request(final String owner, final int priority, final int ticksUntilReset, final T value) {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Request owner is required");
        }
        Objects.requireNonNull(value, "value");
        int lifetime = Math.max(1, ticksUntilReset);
        this.clear(owner);
        this.entries.add(new Entry<>(
                owner,
                priority,
                this.tick + lifetime,
                this.sequence++,
                value
        ));
    }

    public Optional<T> activeValue() {
        return this.active().map(Entry::value);
    }

    public Optional<Entry<T>> active() {
        this.removeExpired();
        return this.entries.stream()
                .max(Comparator.comparingInt(Entry<T>::priority)
                        .thenComparingLong(Entry::sequence));
    }

    public void tick() {
        this.tick++;
        this.removeExpired();
    }

    public void clear(final String owner) {
        this.entries.removeIf(entry -> entry.owner().equals(owner));
    }

    public void clearAll() {
        this.entries.clear();
        this.tick = 0;
        this.sequence = 0;
    }

    public boolean isEmpty() {
        this.removeExpired();
        return this.entries.isEmpty();
    }

    private void removeExpired() {
        this.entries.removeIf(entry -> entry.expiresAt() <= this.tick);
    }

    public record Entry<T>(String owner, int priority, int expiresAt, long sequence, T value) {
    }
}
