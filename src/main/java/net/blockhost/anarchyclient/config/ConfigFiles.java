package net.blockhost.anarchyclient.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File helpers shared by the client's config stores.
 */
public final class ConfigFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFiles.class);
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

    private ConfigFiles() {}

    /**
     * Writes the content to a temporary sibling file, then moves it over the target. A crash during the
     * write leaves the previous file intact.
     */
    public static void writeAtomically(final Path path, final String content) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tempPath, content, StandardCharsets.UTF_8);
        try {
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Moves an unreadable file to a timestamped backup next to it, so the next save cannot overwrite the
     * original data.
     *
     * @return true when the file is gone and saving is safe, false when the backup failed
     */
    public static boolean backUpUnreadable(final Path path) {
        if (!Files.exists(path)) {
            return true;
        }
        Path backup = backupPath(path);
        try {
            try {
                Files.move(path, backup, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException exception) {
                Files.move(path, backup);
            }
            LOGGER.info("Backed up unreadable AnarchyClient file {} to {}", path, backup);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Failed to back up unreadable AnarchyClient file {}", path, exception);
            return false;
        }
    }

    private static Path backupPath(final Path path) {
        String fileName = path.getFileName() == null
                ? "anarchyclient"
                : path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String extension = dot > 0 ? fileName.substring(dot) : "";
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP_FORMAT);
        Path backup = path.resolveSibling(fileName + "." + timestamp + ".backup" + extension);
        int copy = 1;
        while (Files.exists(backup)) {
            backup = path.resolveSibling(fileName + "." + timestamp + "." + copy + ".backup" + extension);
            copy++;
        }
        return backup;
    }
}
