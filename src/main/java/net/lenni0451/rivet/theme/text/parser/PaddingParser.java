package net.lenni0451.rivet.theme.text.parser;

import net.lenni0451.rivet.math.Padding;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.Locale;

@ApiStatus.Internal
public final class PaddingParser implements Parser<Padding> {

    @Nullable
    @Override
    public Padding parse(final String s) {
        String[] parts = s.split(" ");
        Padding padding = this.parseFormat1(parts);
        if (padding == null) {
            padding = this.parseFormat2(parts);
        }
        return padding;
    }

    @Nullable
    private Padding parseFormat1(final String[] parts) {
        if (parts.length != 4) return null;
        float left;
        try {
            left = Float.parseFloat(parts[0]);
        } catch (Throwable t) {
            return null;
        }
        float top;
        try {
            top = Float.parseFloat(parts[1]);
        } catch (Throwable t) {
            return null;
        }
        float right;
        try {
            right = Float.parseFloat(parts[2]);
        } catch (Throwable t) {
            return null;
        }
        float bottom;
        try {
            bottom = Float.parseFloat(parts[3]);
        } catch (Throwable t) {
            return null;
        }
        return new Padding(left, top, right, bottom);
    }

    private Padding parseFormat2(final String[] parts) {
        float left = 0;
        float top = 0;
        float right = 0;
        float bottom = 0;
        for (String part : parts) {
            if (part.toLowerCase(Locale.ROOT).startsWith("left=")) {
                left = Float.parseFloat(part.split("=", 2)[1]);
            } else if (part.toLowerCase(Locale.ROOT).startsWith("top=")) {
                top = Float.parseFloat(part.split("=", 2)[1]);
            } else if (part.toLowerCase(Locale.ROOT).startsWith("right=")) {
                right = Float.parseFloat(part.split("=", 2)[1]);
            } else if (part.toLowerCase(Locale.ROOT).startsWith("bottom=")) {
                bottom = Float.parseFloat(part.split("=", 2)[1]);
            } else {
                throw new IllegalArgumentException("Unknown padding option: " + part);
            }
        }
        return new Padding(left, top, right, bottom);
    }

}
