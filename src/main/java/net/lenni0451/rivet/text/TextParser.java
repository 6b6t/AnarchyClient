package net.lenni0451.rivet.text;

import lombok.experimental.UtilityClass;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.text.format.ColorFormat;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * <h2>Text Format Documentation</h2>
 * <p>
 * This format allows styling text using tags enclosed in angle brackets.
 * Tags do <b>not</b> need to be closed; a style persists until it is explicitly changed or reset.
 * </p>
 *
 * <h3>Basic Syntax</h3>
 * <ul>
 *     <li>
 *         <b>Enable a style:</b> Use the tag name.
 *         <br>Example: <code>&lt;bold&gt;</code>
 *     </li>
 *     <li>
 *         <b>Set a value:</b> Use an equals sign.
 *         <br>Example: <code>&lt;color=red&gt;</code>
 *     </li>
 *     <li>
 *         <b>Reset a style:</b> Use a forward slash prefix to reset a style to its default value.
 *         <br>Example: <code>&lt;/bold&gt;</code>
 *     </li>
 * </ul>
 *
 * <h3>Formatting Options</h3>
 * <p>
 *     The following boolean options are available:
 *     <code>bold</code>, <code>italic</code>, <code>underlined</code>, <code>strikethrough</code>, <code>shadow</code>.
 * </p>
 * <p>
 *     <b>Manual Boolean Values:</b><br>
 *     You can manually set a format to true or false (e.g., <code>&lt;bold=false&gt;</code>).
 *     This is required if you are using a default text format that is already styled.
 *     For example, if the text is italic by default, <code>&lt;/italic&gt;</code> will only reset it to the default (true),
 *     so you must use <code>&lt;italic=false&gt;</code> to render non-italic text.
 * </p>
 *
 * <h3>Color Options</h3>
 * <p>
 *     Use <code>color</code> to set the text color and <code>outline_color</code> to set the outline color.
 * </p>
 *
 * <h4>Supported Color Formats</h4>
 * <ul>
 *     <li>
 *         <b>Names:</b>
 *         <br><code>black</code>, <code>white</code>, <code>red</code>, <code>green</code>, <code>blue</code>,
 *         <code>yellow</code>, <code>cyan</code>, <code>magenta</code>, <code>orange</code>, <code>pink</code>,
 *         <code>gray</code>, <code>light_gray</code>, <code>dark_gray</code>.
 *         <br>Example: <code>&lt;color=red&gt;</code>
 *     </li>
 *     <li>
 *         <b>Hexadecimal:</b>
 *         <br><code>#RRGGBB</code> or <code>#AARRGGBB</code>
 *         <br>Example: <code>&lt;color=#FF0000&gt;</code>
 *     </li>
 *     <li>
 *         <b>Integer (0-255):</b>
 *         <br><code>rgb(r,g,b)</code> or <code>rgba(r,g,b,a)</code>
 *         <br><b>Note:</b> There must be <b>no spaces</b> between parameters.
 *         <br>Example: <code>&lt;color=rgb(100,200,50)&gt;</code>
 *     </li>
 *     <li>
 *         <b>Float (0.0-1.0):</b>
 *         <br><code>rgbf(r,g,b)</code>, <code>rgbaf(r,g,b,a)</code>, or <code>argbf(a,r,g,b)</code>
 *         <br><b>Note:</b> There must be <b>no spaces</b> between parameters.
 *         <br>Example: <code>&lt;color=rgbf(1.0,0.5,0)&gt;</code>
 *     </li>
 * </ul>
 *
 * <h3>Escaping</h3>
 * <p>
 *     To include a literal angle bracket in the text, use a backslash to escape it.
 *     <br>Example: <code>\&lt;This is not a tag\&gt;</code>
 * </p>
 *
 * <h3>Advanced Usage</h3>
 * <p>
 *     Multiple key-value pairs can be combined in a single tag by separating them with spaces.
 *     <br>Example: <code>&lt;color=red bold italic=true&gt;</code>
 * </p>
 */
@UtilityClass
public class TextParser {

    public static TextLine parse(final String text) {
        return parse(text, TextFormat.DEFAULT);
    }

    public static TextLine parse(final String text, final TextFormat defaultFormat) {
        FormatParser parser = new FormatParser(text);
        List<TextSection> sections = new ArrayList<>();
        TextFormat[] currentFormat = {defaultFormat};
        try {
            parser.parse((currentText, newOptions) -> {
                if (!currentText.isEmpty()) {
                    sections.add(new TextSection(currentText, currentFormat[0]));
                }
                for (FormatParser.Option option : newOptions) {
                    currentFormat[0] = applyOption(defaultFormat, currentFormat[0], option);
                }
            });
        } catch (ParserException e) {
            throw new IllegalArgumentException("Failed to parse text: " + e.getMessage(), e);
        }
        return new TextLine(sections);
    }

    private static TextFormat applyOption(final TextFormat defaultFormat, final TextFormat currentFormat, final FormatParser.Option option) throws ParserException {
        return switch (option.name().toLowerCase(Locale.ROOT)) {
            case "color" -> handleColorOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::color, TextFormat::withColor);
            case "outline_color" -> handleColorOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::outlineColor, TextFormat::withOutlineColor);
            case "bold" -> handleBooleanOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::bold, TextFormat::withBold);
            case "italic" -> handleBooleanOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::italic, TextFormat::withItalic);
            case "underlined" -> handleBooleanOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::underlined, TextFormat::withUnderlined);
            case "strikethrough" -> handleBooleanOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::strikethrough, TextFormat::withStrikethrough);
            case "shadow" -> handleBooleanOption(defaultFormat, currentFormat, option.close(), option.value(), TextFormat::shadow, TextFormat::withShadow);
            default -> throw new ParserException("Unknown option name: " + option.name());
        };
    }

    private static TextFormat handleColorOption(final TextFormat defaultFormat, final TextFormat currentFormat, final boolean close, @Nullable String value, final Function<TextFormat, Color> getter, final BiFunction<TextFormat, Color, TextFormat> setter) throws ParserException {
        if (close) {
            return setter.apply(currentFormat, getter.apply(defaultFormat));
        } else {
            if (value == null) throw new ParserException("Color option requires a value");
            value = value.toLowerCase(Locale.ROOT);
            Color color = null;
            try {
                for (ColorFormat colorFormat : ColorFormat.FORMATS) {
                    if (colorFormat.canParse(value)) {
                        color = colorFormat.parse(value);
                        break;
                    }
                }
            } catch (Throwable t) {
                throw new ParserException("Exception during color parsing: " + value, t);
            }
            if (color == null) {
                throw new ParserException("Unknown color format: " + value);
            }
            return setter.apply(currentFormat, color);
        }
    }

    private static TextFormat handleBooleanOption(final TextFormat defaultFormat, final TextFormat currentFormat, final boolean close, @Nullable final String value, final Function<TextFormat, Boolean> getter, final BiFunction<TextFormat, Boolean, TextFormat> setter) {
        if (close) {
            return setter.apply(currentFormat, getter.apply(defaultFormat));
        } else if (value == null || value.equalsIgnoreCase("true")) {
            return setter.apply(currentFormat, true);
        } else {
            return setter.apply(currentFormat, false);
        }
    }

}
