package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.CARD;
import static net.blockhost.anarchyclient.ui.PanelStyle.CARD_HOVER;
import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.FIELD;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_TEXT_Y_FROM_CENTER;
import static net.blockhost.anarchyclient.ui.PanelStyle.LUCIDE_FONT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TRACK;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderCommand;
import net.blockhost.anarchyclient.rivet.RivetInputMapper;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.blockhost.anarchyclient.setting.TextValueSetting;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.impl.FormattedLabel;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.mouse.ClickOn;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.grid.GridLayout;
import net.lenni0451.rivet.layout.grid.GridOptions;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.text.model.TextSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Factory and drawing helpers shared by the client menu components.
 */
final class PanelWidgets {

    static String keyLabel(final int scancode) {
        if (scancode == InputConstants.UNKNOWN.getValue()) {
            return Translations.text("anarchyclient.ui.none", "None");
        }
        return RivetInputMapper.fromScancode(scancode)
                .map(Key::name)
                .orElseGet(() -> Translations.format("anarchyclient.ui.unknown_key", "Key %s", scancode));
    }

    static List<SettingGroup> visibleSettingGroups(final Module module) {
        List<SettingGroup> groups = new ArrayList<>();
        for (SettingGroup group : module.settingGroups()) {
            if (group.visible()) {
                groups.add(group);
            }
        }
        return groups;
    }

    static boolean showGroupHeaders(final List<SettingGroup> groups) {
        return groups.size() > 1
                || (!groups.isEmpty() && !"general".equals(groups.getFirst().id()));
    }

    static Component settingRow(final Setting<?> setting, final Runnable rebuild, final Runnable save) {
        if (setting instanceof BooleanSetting bool) {
            return new BooleanSettingRow(bool, rebuild);
        }
        if (setting instanceof NumberSetting number) {
            return new NumberSettingRow(number, save);
        }
        if (setting instanceof SelectSetting select) {
            return new SelectSettingRow(select, rebuild);
        }
        if (setting instanceof TextValueSetting text) {
            return new TextSettingRow(setting, text, save);
        }
        return new ValueSettingRow(setting);
    }

    static String fitText(final Component component, final String value, final Color color, final float maxWidth) {
        if (maxWidth <= 0F || value.isEmpty()) {
            return "";
        }
        if (textWidth(component, value, color) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        if (textWidth(component, ellipsis, color) > maxWidth) {
            return "";
        }
        return value.substring(0, fitLength(component, value, color, maxWidth, ellipsis))
                        .stripTrailing()
                + ellipsis;
    }

    /** Longest prefix of {@code value} that, with {@code suffix} appended, still fits {@code maxWidth}. */
    static int fitLength(
            final Component component,
            final String value,
            final Color color,
            final float maxWidth,
            final String suffix) {
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = value.substring(0, middle).stripTrailing() + suffix;
            if (textWidth(component, candidate, color) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /**
     * Greedy word wrap: break at spaces while lines fit, ellipsize only the final allowed line.
     */
    static List<String> wrapText(
            final Component component,
            final String value,
            final Color color,
            final float maxWidth,
            final int maxLines) {
        if (maxWidth <= 0F || value.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>(Math.min(maxLines, 8));
        String remaining = value.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (lines.size() == maxLines - 1) {
                lines.add(fitText(component, remaining, color, maxWidth));
                break;
            }
            if (textWidth(component, remaining, color) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int fit = fitLength(component, remaining, color, maxWidth, "");
            int breakAt = remaining.lastIndexOf(' ', fit);
            if (breakAt <= 0) {
                breakAt = Math.max(1, fit);
            }
            lines.add(remaining.substring(0, breakAt).stripTrailing());
            remaining = remaining.substring(breakAt).stripLeading();
        }
        return lines;
    }

    static float textWidth(final Component component, final String value, final Color color) {
        return component
                .rivet()
                .backend()
                .font()
                .shapeText(value, color)
                .logicalBounds()
                .width();
    }

    static GridCell fillCell(final int column, final int row) {
        return cell(column, row, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    static GridCell fillCell(final int column, final int row, final int columnSpan, final int rowSpan) {
        return cell(
                column, row, columnSpan, rowSpan, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    static GridCell fixedCell(
            final int column, final int row, final float width, final float height, final GridAnchor anchor) {
        return cell(column, row, 1, 1, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    static GridCell fixedCell(
            final int column,
            final int row,
            final int columnSpan,
            final int rowSpan,
            final float width,
            final float height,
            final GridAnchor anchor) {
        return cell(column, row, columnSpan, rowSpan, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    static GridCell cell(
            final int column,
            final int row,
            final int columnSpan,
            final int rowSpan,
            final float weightX,
            final float weightY,
            final GridAnchor anchor,
            final GridFill fill,
            final Padding padding,
            final Float width,
            final Float height) {
        // Rivet's GridLayout uses child ideal sizes as row/column minimums. Flexible cells
        // need explicit zero bases so text and scroll content cannot push fixed UI outside.
        Float effectiveWidth = width;
        if (effectiveWidth == null && weightX > 0F) {
            effectiveWidth = 0F;
        }
        Float effectiveHeight = height;
        if (effectiveHeight == null && weightY > 0F) {
            effectiveHeight = 0F;
        }
        return new GridCell(
                new GridOptions(column, row, columnSpan, rowSpan, weightX, weightY, anchor, fill, padding),
                effectiveWidth,
                effectiveHeight);
    }

    static void place(final Component component, final GridCell cell) {
        GridFill fill = cell.options().fill();
        if (cell.width() != null) {
            if (fill == GridFill.HORIZONTAL || fill == GridFill.BOTH) {
                component.minSize(cell.width(), component.minSize().height());
            } else {
                component.fixedSize(cell.width(), -1);
            }
        }
        if (cell.height() != null) {
            if (fill == GridFill.VERTICAL || fill == GridFill.BOTH) {
                component.minSize(component.minSize().width(), cell.height());
            } else {
                component.fixedSize(-1, cell.height());
            }
        }
        component.layoutOptions(cell.options());
    }

    static GridLayout gridLayout(final int horizontalGap, final int verticalGap) {
        return new GridLayout(horizontalGap, verticalGap, false, false, true, true);
    }

    static Padding padding(final float left, final float top, final float right, final float bottom) {
        return new Padding(left, top, right, bottom);
    }

    static FormattedLabel label(final String text, final Color color) {
        FormattedLabel label =
                new FormattedLabel(new TextLine(new TextSection(text, TextFormat.DEFAULT.withColor(color))));
        label.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT);
        label.verticalOrigin(TextOrigin.Vertical.LOGICAL_CENTER);
        label.capabilities().all(false);
        return label;
    }

    static Button button(final String text, final Color color, final Button.ClickListener listener) {
        Button button = new Button(label(text, color), listener);
        button.cornerRadius().set(8F);
        button.outlineWidth().set(1F);
        button.inactiveColor().set(CARD);
        button.inactiveOutlineColor().set(DIVIDER);
        button.activeColor().set(CARD_HOVER);
        button.activeOutlineColor().set(Color.fromRGBA(255, 255, 255, 60));
        button.clickColor().set(Color.fromRGBA(255, 255, 255, 10));
        button.clickOutlineColor().set(GlassTheme::accent);
        button.innerPadding().set(new Padding(8, 4, 8, 4));
        button.clickOn().set(ClickOn.UP);
        return button;
    }

    static TextField textField(final String value) {
        TextField field = new TextField(value);
        field.backgroundColor().set(FIELD);
        field.outlineColor().set(DIVIDER);
        field.focusedOutlineColor().set(GlassTheme::accent);
        field.selectionColor().set(() -> GlassTheme.accent().multiplyAlpha(0.4F));
        field.cursorColor().set(TEXT);
        field.cornerRadius().set(8F);
        field.innerPadding().set(new Padding(7, 4, 7, 4));
        return field;
    }

    /** Runs {@code action} when Enter (or numpad Enter) is pressed while the field is focused. */
    static void onEnter(final TextField field, final Runnable action) {
        field.keyDownListener().add(event -> {
            if (event.key().isEquivalent(Key.ENTER) || event.key().isEquivalent(Key.KP_ENTER)) {
                action.run();
                return true;
            }
            return false;
        });
    }

    static Slider slider(final double min, final double max, final double step, final double value) {
        Slider slider = new Slider(min, max, step, value);
        slider.barColor().set(TRACK);
        slider.activeBarColor().set(GlassTheme::accent);
        slider.thumbColor().set(TEXT);
        slider.thumbClickColor().set(GlassTheme::accent);
        slider.barHeight().set(3F);
        slider.barCornerRadius().set(1.5F);
        slider.thumbWidth().set(9F);
        slider.thumbHeight().set(9F);
        slider.thumbCornerRadius().set(4.5F);
        slider.fixedSize(-1, 16);
        return slider;
    }

    static void drawText(
            final Component component,
            final Renderer renderer,
            final String text,
            final Color color,
            final float x,
            final float y,
            final TextOrigin.Horizontal horizontal,
            final TextOrigin.Vertical vertical) {
        renderer.text(component.rivet().backend().font().shapeText(text, color), x, y, horizontal, vertical);
    }

    /**
     * Draws a Lucide icon centered on {@code (centerX, centerY)}. The X axis is centered exactly
     * using the measured glyph width; the Y axis uses the single {@link PanelStyle#ICON_TEXT_Y_FROM_CENTER}
     * calibration so every icon sits the same way instead of each call site guessing.
     */
    static void drawIcon(
            final Renderer renderer, final String icon, final float centerX, final float centerY, final Color color) {
        String glyph = LucideIcons.glyph(icon);
        if (glyph.isEmpty()) {
            return;
        }
        net.minecraft.network.chat.Component component =
                net.minecraft.network.chat.Component.literal(glyph).withStyle(style -> style.withFont(LUCIDE_FONT));
        float width = Minecraft.getInstance().font.width(component);
        float left = centerX - width / 2F;
        float top = centerY - ICON_TEXT_Y_FROM_CENTER;
        renderer.custom(new Blaze3DRenderCommand(
                (GuiGraphicsExtractor graphics) -> graphics.text(
                        Minecraft.getInstance().font, component, Math.round(left), Math.round(top), argb(color), false),
                new Rectangle(0F, 0F, ICON_BOX, ICON_BOX)));
    }

    static void drawFittedText(
            final Component component,
            final Renderer renderer,
            final String text,
            final Color color,
            final float x,
            final float y,
            final float maxWidth,
            final TextOrigin.Horizontal horizontal,
            final TextOrigin.Vertical vertical) {
        String fitted = fitText(component, text, color, maxWidth);
        if (!fitted.isEmpty()) {
            drawText(component, renderer, fitted, color, x, y, horizontal, vertical);
        }
    }

    /** Wrapping description text for a setting row, or {@code null} when the setting has none. */
    static TextNode settingDescription(final Setting<?> setting) {
        String description = setting.description().strip();
        return description.isEmpty() ? null : textNode(description, FAINT).maxLines(Integer.MAX_VALUE);
    }

    static TextNode textNode(final String text, final Color color) {
        return new TextNode(() -> text, () -> color);
    }

    static TextNode textNode(final String text, final Supplier<Color> color) {
        return new TextNode(() -> text, color);
    }

    static TextNode textNode(final Supplier<String> text, final Supplier<Color> color) {
        return new TextNode(text, color);
    }

    static IconNode iconNode(final String icon, final Color color) {
        return new IconNode(() -> icon, () -> color);
    }

    static IconNode iconNode(final String icon, final Supplier<Color> color) {
        return new IconNode(() -> icon, color);
    }

    static IconNode iconNode(final Supplier<String> icon, final Supplier<Color> color) {
        return new IconNode(icon, color);
    }

    static IconButton iconButton(final String icon, final Supplier<Color> color, final Runnable action) {
        return iconButton(icon, color, action, () -> true);
    }

    static IconButton iconButton(
            final String icon, final Supplier<Color> color, final Runnable action, final BooleanSupplier visible) {
        return new IconButton(() -> icon, color, action, visible);
    }

    static TextAction textAction(final String text, final Color color, final Runnable action) {
        return new TextAction(() -> text, () -> color, action);
    }

    static TextAction textAction(final String text, final Supplier<Color> color, final Runnable action) {
        return new TextAction(() -> text, color, action);
    }

    static Surface surface(final Color color) {
        return new Surface(() -> color);
    }

    static Surface surface(final Supplier<Color> color) {
        return new Surface(color);
    }

    static String categoryIcon(final ModuleCategory category) {
        return switch (category) {
            case COMBAT -> "crosshair";
            case RENDER -> "eye";
            case MOVEMENT -> "move";
            case WORLD -> "globe";
            case PLAYER -> "user";
            case HUD -> "layout";
            case MISC -> "package";
            case FUN -> "zap";
        };
    }

    static int argb(final Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }

    private PanelWidgets() {}
}
