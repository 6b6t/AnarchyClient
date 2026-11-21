package net.blockhost.anarchyclient.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** File helpers that {@link Files} does not have. */
public final class MoreFiles {

    private MoreFiles() {}

    /** Creates the directory that will hold {@code file}, and any missing parents. */
    public static void createParentDirectories(final Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
