package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class BookBotModule extends Module {

    private final StringSetting pages = this.setting(StringSetting.from(StringSetting.builder()
            .id("pages")
            .name("Pages")
            .defaultValue("AnarchyClient page 1|AnarchyClient page 2")
            .description("Separate pages with |.")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(20.0)
            .min(0.0)
            .max(200.0)
            .step(5.0)
            .build()));
    private final BooleanSetting sign = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sign")
            .name("Sign")
            .defaultValue(false)
            .build()));
    private final StringSetting title = this.setting(StringSetting.from(StringSetting.builder()
            .id("title")
            .name("Title")
            .defaultValue("AnarchyClient")
            .build()));
    private final BooleanSetting loop = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("loop")
            .name("Loop")
            .defaultValue(false)
            .build()));
    private int cooldownTicks;
    private int booksWritten;

    public BookBotModule() {
        super("book_bot", "Book Bot", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        List<String> configuredPages = parsePages(this.pages.value());
        if (player == null || client.getConnection() == null || configuredPages.isEmpty()) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        Optional<BookTarget> target = findBookTarget(player);
        if (target.isEmpty()) {
            this.debugValue("status", "missing writable book");
            this.cooldownTicks = this.delay.value().intValue();
            return;
        }
        if (target.orElseThrow().hotbarSlot().isPresent()) {
            SilentHotbar.select(player, this.id(), target.orElseThrow().hotbarSlot().orElseThrow(),
                    SilentHotbar.PRIORITY_NORMAL, 2, true);
        }
        List<String> bookPages = sanitizePages(configuredPages);
        Optional<String> bookTitle = this.sign.value() ? Optional.of(sanitizeTitle(this.title.value())) : Optional.empty();
        client.getConnection().send(new ServerboundEditBookPacket(target.orElseThrow().packetSlot(), bookPages, bookTitle));
        this.booksWritten++;
        this.debugValue("status", this.sign.value() ? "signed" : "written");
        this.debugValue("books", this.booksWritten);
        this.debugValue("pages", bookPages.size());
        if (!this.loop.value()) {
            this.enabled(false);
            return;
        }
        this.cooldownTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
        this.booksWritten = 0;
        SilentHotbar.clear(this.id());
        this.clearDebugValues();
    }

    private static Optional<BookTarget> findBookTarget(final LocalPlayer player) {
        int selectedSlot = player.getInventory().getSelectedSlot();
        if (isWritableBook(player.getMainHandItem())) {
            return Optional.of(new BookTarget(selectedSlot, OptionalInt.empty()));
        }
        if (isWritableBook(player.getOffhandItem())) {
            return Optional.of(new BookTarget(Inventory.SLOT_OFFHAND, OptionalInt.empty()));
        }
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (isWritableBook(stack)) {
                return Optional.of(new BookTarget(slot, OptionalInt.of(slot)));
            }
        }
        return Optional.empty();
    }

    static boolean isWritableBook(final ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.WRITABLE_BOOK) || stack.has(DataComponents.WRITABLE_BOOK_CONTENT));
    }

    static List<String> sanitizePages(final List<String> pages) {
        List<String> sanitized = new ArrayList<>();
        for (String page : pages) {
            if (sanitized.size() >= WritableBookContent.MAX_PAGES) {
                break;
            }
            sanitized.add(limit(page == null ? "" : page, WritableBookContent.PAGE_EDIT_LENGTH));
        }
        return sanitized.isEmpty() ? List.of("") : List.copyOf(sanitized);
    }

    static List<String> parsePages(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\|", -1))
                .map(String::trim)
                .toList();
    }

    static String sanitizeTitle(final String title) {
        String sanitized = title == null || title.isBlank() ? "AnarchyClient" : title.trim();
        return limit(sanitized, WrittenBookContent.TITLE_MAX_LENGTH);
    }

    private static String limit(final String value, final int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record BookTarget(int packetSlot, OptionalInt hotbarSlot) {

        private BookTarget {
            if (packetSlot == Inventory.SLOT_OFFHAND) {
                packetSlot = 40;
            }
        }
    }
}
