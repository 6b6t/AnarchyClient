package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class ItemScan {

    private ItemScan() {
    }

    static Set<Item> parseItems(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<Item> items = new LinkedHashSet<>();
        for (String token : value.split("[,|\\s]+")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            Identifier identifier = id.contains(":") ? Identifier.tryParse(id) : Identifier.withDefaultNamespace(id);
            if (identifier != null) {
                BuiltInRegistries.ITEM.getOptional(identifier).ifPresent(items::add);
            }
        }
        return Set.copyOf(items);
    }
}
