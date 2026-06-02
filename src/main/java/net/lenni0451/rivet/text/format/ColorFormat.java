package net.lenni0451.rivet.text.format;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.text.ParserException;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface ColorFormat {

    List<ColorFormat> FORMATS = List.of(
            new HexColorFormat(3, value -> Color.fromRGB(value[0], value[1], value[2])),
            new HexColorFormat(4, value -> Color.fromARGB(value[0], value[1], value[2], value[3])),
            new IntColorFormat("rgb", 3, value -> Color.fromRGB(value[0], value[1], value[2])),
            new IntColorFormat("rgba", 4, value -> Color.fromRGBA(value[0], value[1], value[2], value[3])),
            new IntColorFormat("argb", 4, value -> Color.fromARGB(value[0], value[1], value[2], value[3])),
            new FloatColorFormat("rgbf", 3, Color::fromRGBF),
            new FloatColorFormat("rgbaf", 4, Color::fromRGBAF),
            new FloatColorFormat("argbf", 4, value -> Color.fromARGBF(value[0], value[1], value[2], value[3])),
            new StaticColorFormat("black", Color.BLACK),
            new StaticColorFormat("light_gray", Color.LIGHT_GRAY),
            new StaticColorFormat("gray", Color.GRAY),
            new StaticColorFormat("dark_gray", Color.DARK_GRAY),
            new StaticColorFormat("white", Color.WHITE),
            new StaticColorFormat("red", Color.RED),
            new StaticColorFormat("green", Color.GREEN),
            new StaticColorFormat("blue", Color.BLUE),
            new StaticColorFormat("orange", Color.ORANGE),
            new StaticColorFormat("yellow", Color.YELLOW),
            new StaticColorFormat("cyan", Color.CYAN),
            new StaticColorFormat("pink", Color.PINK),
            new StaticColorFormat("magenta", Color.MAGENTA)
    );

    boolean canParse(final String s);

    Color parse(final String s) throws ParserException;

}
