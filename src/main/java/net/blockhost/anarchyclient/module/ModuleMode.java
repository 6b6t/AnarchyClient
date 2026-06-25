package net.blockhost.anarchyclient.module;

/**
 * Describes a named behavior branch inside a module.
 *
 * <p>AnarchyClient mostly stores modes as select settings, but keeping the
 * mode vocabulary in a tiny value type makes tests and profile rules less
 * dependent on display text.</p>
 */
public record ModuleMode(String id, String name) {

    public ModuleMode {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Mode id cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Mode name cannot be blank");
        }
    }
}
