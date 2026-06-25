package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ColorSignsModule extends Module {

    private static final Pattern COLOR_CODE = Pattern.compile("(?i)&([0-9A-FK-OR])");

    private final BooleanSetting signs = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("signs")
            .name("Signs")
            .defaultValue(true)
            .build()));
    private final BooleanSetting books = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("books")
            .name("Books")
            .defaultValue(false)
            .build()));

    public ColorSignsModule() {
        super("color_signs", "Color Signs", ModuleCategory.MISC);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.signs.value() && packet instanceof ServerboundSignUpdatePacket signPacket) {
            String[] lines = signPacket.getLines();
            String[] colored = colorizeLines(lines);
            if (colored != lines) {
                return new ServerboundSignUpdatePacket(signPacket.getPos(), signPacket.isFrontText(),
                        colored[0], colored[1], colored[2], colored[3]);
            }
        }
        if (this.books.value() && packet instanceof ServerboundEditBookPacket bookPacket) {
            List<String> pages = colorizePages(bookPacket.pages());
            Optional<String> title = bookPacket.title().map(ColorSignsModule::colorize);
            if (pages != bookPacket.pages() || !title.equals(bookPacket.title())) {
                return new ServerboundEditBookPacket(bookPacket.slot(), pages, title);
            }
        }
        return packet;
    }

    static String colorize(final String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text;
        }
        return COLOR_CODE.matcher(text).replaceAll("\u00a7$1");
    }

    private static String[] colorizeLines(final String[] lines) {
        String[] colored = lines.clone();
        boolean changed = false;
        for (int index = 0; index < colored.length; index++) {
            String line = colorize(colored[index]);
            changed |= !line.equals(colored[index]);
            colored[index] = line;
        }
        return changed ? colored : lines;
    }

    private static List<String> colorizePages(final List<String> pages) {
        List<String> colored = new ArrayList<>(pages.size());
        boolean changed = false;
        for (String page : pages) {
            String colorized = colorize(page);
            changed |= !colorized.equals(page);
            colored.add(colorized);
        }
        return changed ? List.copyOf(colored) : pages;
    }
}
