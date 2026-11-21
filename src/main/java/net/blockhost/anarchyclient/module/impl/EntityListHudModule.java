package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

public final class EntityListHudModule extends HudElementModule {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(64.0)
            .min(4.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxLines = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_lines")
            .name("Max Lines")
            .defaultValue(8.0)
            .min(1.0)
            .max(30.0)
            .step(1.0)
            .build()));
    private final BooleanSetting items = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue(true)
            .description("Lists dropped items by item name instead of as one Item entry.")
            .build()));

    public EntityListHudModule() {
        super(
                "entity_list_hud",
                "Entity List HUD",
                "Lists the entities near you grouped by type, with a count for each, most common first.",
                "Top Right");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        if (client.level == null) {
            return List.of();
        }
        double rangeSqr = this.range.value() * this.range.value();
        Map<String, Integer> counts = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player || client.player.distanceToSqr(entity) > rangeSqr) {
                continue;
            }
            counts.merge(label(entity, this.items.value()), count(entity, this.items.value()), Integer::sum);
        }
        return format(counts, this.maxLines.value().intValue());
    }

    private static String label(final Entity entity, final boolean itemNames) {
        if (itemNames && entity instanceof ItemEntity item) {
            return item.getItem().getHoverName().getString();
        }
        return entity.getType().getDescription().getString();
    }

    /** Named item rows count items in the stacks; the shared Item row counts dropped entities. */
    private static int count(final Entity entity, final boolean itemNames) {
        return itemNames && entity instanceof ItemEntity item ? item.getItem().getCount() : 1;
    }

    /** Sorts entries by count, then name, and adds a summary line when some do not fit. */
    static List<String> format(final Map<String, Integer> counts, final int maxLines) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));
        List<String> lines = new ArrayList<>();
        int shown = Math.min(entries.size(), Math.max(1, maxLines));
        for (int index = 0; index < shown; index++) {
            Map.Entry<String, Integer> entry = entries.get(index);
            lines.add(entry.getKey() + " x" + entry.getValue());
        }
        if (entries.size() > shown) {
            lines.add("+" + (entries.size() - shown) + " more");
        }
        return lines;
    }
}
