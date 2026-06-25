package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class SoundScan {

    private SoundScan() {
    }

    static Set<Identifier> parseSoundIds(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<Identifier> sounds = new LinkedHashSet<>();
        for (String token : value.split("[,|\\s]+")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty()) {
                continue;
            }
            Identifier identifier = id.contains(":") ? Identifier.tryParse(id) : Identifier.withDefaultNamespace(id);
            if (identifier != null && BuiltInRegistries.SOUND_EVENT.containsKey(identifier)) {
                sounds.add(identifier);
            }
        }
        return Set.copyOf(sounds);
    }
}
