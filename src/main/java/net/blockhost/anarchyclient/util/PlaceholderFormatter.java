package net.blockhost.anarchyclient.util;

import java.util.Map;

public final class PlaceholderFormatter {

    private PlaceholderFormatter() {
    }

    public static String format(final String template, final Map<String, String> values) {
        if (template == null || template.isEmpty() || values == null || values.isEmpty()) {
            return template == null ? "" : template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}
