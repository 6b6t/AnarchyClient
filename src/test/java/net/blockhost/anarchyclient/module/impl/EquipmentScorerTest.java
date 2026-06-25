package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentScorerTest {

    @Test
    void parsesAvoidedEnchantmentIdentifiers() {
        assertEquals(Set.of("binding_curse", "minecraft:frost_walker", "vanishing_curse"),
                EquipmentScorer.parseIdentifiers("binding_curse, minecraft:frost_walker | vanishing_curse"));
    }

    @Test
    void matchesIdentifiersByFullIdOrPath() {
        assertTrue(EquipmentScorer.matchesAny("minecraft:binding_curse", Set.of("binding_curse")));
        assertTrue(EquipmentScorer.matchesAny("minecraft:binding_curse", Set.of("minecraft:binding_curse")));
        assertFalse(EquipmentScorer.matchesAny("minecraft:protection", Set.of("binding_curse")));
    }

    @Test
    void mapsProtectionSettingValues() {
        assertEquals(EquipmentScorer.ProtectionPreference.PROTECTION,
                EquipmentScorer.ProtectionPreference.fromSetting("Protection"));
        assertEquals(EquipmentScorer.ProtectionPreference.BLAST,
                EquipmentScorer.ProtectionPreference.fromSetting("Blast"));
        assertEquals(EquipmentScorer.ProtectionPreference.FIRE,
                EquipmentScorer.ProtectionPreference.fromSetting("Fire"));
        assertEquals(EquipmentScorer.ProtectionPreference.PROJECTILE,
                EquipmentScorer.ProtectionPreference.fromSetting("Projectile"));
    }
}
