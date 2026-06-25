package net.blockhost.anarchyclient.setting;

public record SettingColor(int red, int green, int blue, int alpha) {

    public SettingColor {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
        alpha = clamp(alpha);
    }

    public static SettingColor rgb(final int red, final int green, final int blue) {
        return new SettingColor(red, green, blue, 255);
    }

    public static SettingColor parse(final String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() == 6 || normalized.length() == 8) {
            try {
                int red = Integer.parseInt(normalized.substring(0, 2), 16);
                int green = Integer.parseInt(normalized.substring(2, 4), 16);
                int blue = Integer.parseInt(normalized.substring(4, 6), 16);
                int alpha = normalized.length() == 8 ? Integer.parseInt(normalized.substring(6, 8), 16) : 255;
                return new SettingColor(red, green, blue, alpha);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA.", exception);
            }
        }
        String[] parts = normalized.split("[,;\\s]+");
        if (parts.length == 3 || parts.length == 4) {
            try {
                return new SettingColor(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        parts.length == 4 ? Integer.parseInt(parts[3]) : 255
                );
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected numeric RGBA channels.", exception);
            }
        }
        throw new IllegalArgumentException("Expected #RRGGBB, #RRGGBBAA, or r,g,b,a.");
    }

    public String hex() {
        if (this.alpha == 255) {
            return "#%02x%02x%02x".formatted(this.red, this.green, this.blue);
        }
        return "#%02x%02x%02x%02x".formatted(this.red, this.green, this.blue, this.alpha);
    }

    public int argb() {
        return this.alpha << 24 | this.red << 16 | this.green << 8 | this.blue;
    }

    private static int clamp(final int value) {
        return Math.max(0, Math.min(255, value));
    }
}
