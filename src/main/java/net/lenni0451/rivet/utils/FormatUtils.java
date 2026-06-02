package net.lenni0451.rivet.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormatUtils {

    public static int getDecimalPlaces(final double d) {
        // Max 6 decimal places
        return Math.max(0, BigDecimal.valueOf(d).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().scale());
    }

    public static String formatDecimalString(final String format, final double d) {
        int decimalPlaces = getDecimalPlaces(d);
        return format.replaceAll("%([0-9$,\\-+ ]*)f", "%$1." + decimalPlaces + "f");
    }

}
