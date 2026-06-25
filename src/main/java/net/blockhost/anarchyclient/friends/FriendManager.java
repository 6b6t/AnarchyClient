package net.blockhost.anarchyclient.friends;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class FriendManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FriendManager.class);
    private static final Pattern NAME_SPLIT = Pattern.compile("[,|\\s]+");

    private final Map<String, String> friends = new LinkedHashMap<>();
    private final Path path;

    public FriendManager(final Path path) {
        this.path = path;
    }

    public void load() {
        this.friends.clear();
        if (!Files.exists(this.path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(this.path, StandardCharsets.UTF_8)) {
                this.addLoaded(line);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to load AnarchyClient friends from {}", this.path, exception);
        }
    }

    public boolean isFriend(final String name) {
        return key(name).map(this.friends::containsKey).orElse(false);
    }

    public List<String> friends() {
        return List.copyOf(this.friends.values());
    }

    public boolean add(final String name) {
        Optional<String> key = key(name);
        if (key.isEmpty()) {
            return false;
        }
        String displayName = displayName(name);
        if (displayName.equals(this.friends.put(key.orElseThrow(), displayName))) {
            return false;
        }
        this.save();
        return true;
    }

    public int addAll(final Collection<String> names) {
        int added = 0;
        for (String name : names) {
            Optional<String> key = key(name);
            if (key.isEmpty()) {
                continue;
            }
            String previous = this.friends.putIfAbsent(key.orElseThrow(), displayName(name));
            if (previous == null) {
                added++;
            }
        }
        if (added > 0) {
            this.save();
        }
        return added;
    }

    public boolean remove(final String name) {
        Optional<String> key = key(name);
        if (key.isEmpty() || this.friends.remove(key.orElseThrow()) == null) {
            return false;
        }
        this.save();
        return true;
    }

    public static List<String> parseNames(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String token : NAME_SPLIT.split(value)) {
            String displayName = displayName(token);
            if (!displayName.isEmpty()) {
                names.add(displayName);
            }
        }
        return List.copyOf(names);
    }

    private void addLoaded(final String name) {
        key(name).ifPresent(key -> this.friends.put(key, displayName(name)));
    }

    private void save() {
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempPath = this.path.resolveSibling(this.path.getFileName() + ".tmp");
            String content = String.join("\n", this.friends.values());
            if (!content.isEmpty()) {
                content += "\n";
            }
            Files.writeString(tempPath, content, StandardCharsets.UTF_8);
            try {
                Files.move(tempPath, this.path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(tempPath, this.path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to save AnarchyClient friends to {}", this.path, exception);
        }
    }

    private static Optional<String> key(final String name) {
        String displayName = displayName(name);
        if (displayName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(displayName.toLowerCase(Locale.ROOT));
    }

    private static String displayName(final String name) {
        return name == null ? "" : name.trim();
    }
}
