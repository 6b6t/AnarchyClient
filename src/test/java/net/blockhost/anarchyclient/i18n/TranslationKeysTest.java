package net.blockhost.anarchyclient.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Keeps the English language file in sync with the code, and checks every translation against it.
 *
 * <p>Run with {@code ANARCHYCLIENT_WRITE_LANG=1} to rewrite {@code en_us.json} from the code.
 */
@ExtendWith(MinecraftBootstrapExtension.class)
class TranslationKeysTest {

    private static final Path LANG_DIR = Path.of("src/main/resources/assets/anarchyclient/lang");
    private static final Path SOURCE_DIR = Path.of("src/main/java");
    /** Keys vanilla systems look up directly rather than through {@link Translations}. */
    private static final Map<String, String> FIXED_KEYS = Map.of("key.anarchyclient.open_menu", "Open AnarchyClient");

    private static final Pattern LOOKUP = Pattern.compile(
            "Translations\\.(?:text|format|component)\\(\\s*\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?s");
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Test
    void englishFileListsEveryKeyInTheCode() throws IOException {
        Map<String, String> expected = expectedEnglish();
        Path english = LANG_DIR.resolve("en_us.json");
        if ("1".equals(System.getenv("ANARCHYCLIENT_WRITE_LANG"))) {
            Files.writeString(english, GSON.toJson(expected) + "\n", StandardCharsets.UTF_8);
        }
        assertEquals(
                expected, read(english), "en_us.json is out of date; run the tests with ANARCHYCLIENT_WRITE_LANG=1");
    }

    @Test
    void translationsOnlyUseKnownKeysWithMatchingPlaceholders() throws IOException {
        Map<String, String> english = read(LANG_DIR.resolve("en_us.json"));
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.list(LANG_DIR)) {
            for (Path file : files.filter(path -> !path.endsWith("en_us.json")).toList()) {
                for (Map.Entry<String, String> entry : read(file).entrySet()) {
                    String source = english.get(entry.getKey());
                    if (source == null) {
                        problems.add(file.getFileName() + ": unknown key " + entry.getKey());
                    } else if (placeholders(source) != placeholders(entry.getValue())) {
                        problems.add(file.getFileName() + ": placeholders differ for " + entry.getKey());
                    }
                }
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    private static Map<String, String> expectedEnglish() throws IOException {
        Map<String, String> entries = new TreeMap<>(FIXED_KEYS);
        for (ModuleCategory category : ModuleCategory.values()) {
            entries.put(category.translationKey(), category.englishName());
        }
        ModuleManager modules = new ModuleManager();
        ModuleRegistry.registerDefaults(modules);
        for (Module module : modules.all()) {
            entries.put(module.descriptionKey(), module.englishDescription());
            for (SettingGroup group : module.settingGroups()) {
                entries.put(group.translationKey(), group.englishName());
            }
            for (Setting<?> setting : module.settings()) {
                entries.put(setting.translationKey(), setting.englishName());
                if (!setting.englishDescription().isEmpty()) {
                    entries.put(setting.translationKey() + ".description", setting.englishDescription());
                }
                if (setting instanceof SelectSetting select) {
                    for (String option : select.options()) {
                        entries.put(SelectSetting.optionKey(setting.translationKey(), option), option);
                    }
                }
            }
        }
        try (Stream<Path> sources = Files.walk(SOURCE_DIR)) {
            for (Path source :
                    sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = LOOKUP.matcher(Files.readString(source));
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String text = unescape(matcher.group(2));
                    String previous = entries.put(key, text);
                    if (previous != null && !previous.equals(text)) {
                        throw new AssertionError(
                                "Key " + key + " has two English texts: \"" + previous + "\" and \"" + text + "\"");
                    }
                }
            }
        }
        return entries;
    }

    private static String unescape(final String javaLiteral) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < javaLiteral.length(); index++) {
            char c = javaLiteral.charAt(index);
            if (c != '\\' || index + 1 == javaLiteral.length()) {
                builder.append(c);
                continue;
            }
            char next = javaLiteral.charAt(++index);
            switch (next) {
                case 'n' -> builder.append('\n');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    builder.append((char) Integer.parseInt(javaLiteral.substring(index + 1, index + 5), 16));
                    index += 4;
                }
                default -> builder.append(next);
            }
        }
        return builder.toString();
    }

    private static int placeholders(final String text) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static Map<String, String> read(final Path file) throws IOException {
        JsonObject json = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
        Map<String, String> entries = new TreeMap<>();
        json.entrySet()
                .forEach(entry -> entries.put(entry.getKey(), entry.getValue().getAsString()));
        return entries;
    }
}
