package au.org.raid.iam.theme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * RAID-1068: every piece of text and every link on the login page comes from a
 * message key, so a realm can change it under Realm settings, Localization,
 * Realm overrides. These checks keep the Freemarker templates and the theme
 * message bundles in step.
 *
 * ardc-branding inherits raid-custom, and message bundles merge per key, so a
 * key counts as defined if either theme defines it.
 */
class ThemeMessagesTest {

    private static final Path THEMES = Path.of("themes");
    private static final List<String> THEME_NAMES = List.of("raid-custom", "ardc-branding");

    // Keys the realm supplies and the templates do not define themselves are not
    // allowed: a missing key renders as its own name on the page.
    private static final Pattern MSG_KEY = Pattern.compile("msg\\(\\s*['\"]([^'\"]+)['\"]");

    private static Path loginDir(String theme) {
        return THEMES.resolve(theme).resolve("login");
    }

    private static List<Path> templates(String theme) throws IOException {
        try (Stream<Path> files = Files.list(loginDir(theme))) {
            return files.filter(p -> p.getFileName().toString().endsWith(".ftl")).sorted().collect(Collectors.toList());
        }
    }

    private static Properties bundle(String theme) throws IOException {
        Properties properties = new Properties();
        Path file = loginDir(theme).resolve("messages").resolve("messages_en.properties");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static Set<String> definedKeys(String theme) throws IOException {
        Set<String> keys = new TreeSet<>(bundle("raid-custom").stringPropertyNames());
        if (!theme.equals("raid-custom")) {
            keys.addAll(bundle(theme).stringPropertyNames());
        }
        return keys;
    }

    private static Set<String> usedKeys(String theme) throws IOException {
        Set<String> keys = new TreeSet<>();
        for (Path template : templates(theme)) {
            Matcher matcher = MSG_KEY.matcher(Files.readString(template, StandardCharsets.UTF_8));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    private static String withoutComments(String source) {
        return source
                .replaceAll("(?s)<#--.*?-->", "")
                .replaceAll("(?s)<!--.*?-->", "");
    }

    @Test
    @DisplayName("every message key a template uses is defined by the theme")
    void everyUsedKeyIsDefined() throws IOException {
        List<String> missing = new ArrayList<>();
        for (String theme : THEME_NAMES) {
            Set<String> defined = definedKeys(theme);
            for (String key : usedKeys(theme)) {
                if (!defined.contains(key)) {
                    missing.add(theme + ": " + key);
                }
            }
        }
        assertEquals(List.of(), missing, "keys used in a template but defined in no message bundle");
    }

    @Test
    @DisplayName("every message key a theme defines is used by a template")
    void everyDefinedKeyIsUsed() throws IOException {
        Set<String> used = new TreeSet<>();
        for (String theme : THEME_NAMES) {
            used.addAll(usedKeys(theme));
        }
        List<String> unused = new ArrayList<>();
        for (String theme : THEME_NAMES) {
            for (String key : bundle(theme).stringPropertyNames()) {
                if (!used.contains(key)) {
                    unused.add(theme + ": " + key);
                }
            }
        }
        unused.sort(String::compareTo);
        assertEquals(List.of(), unused, "keys defined in a message bundle but used by no template");
    }

    @Test
    @DisplayName("ardc-branding defines only keys that raid-custom also defines")
    void ardcOverridesOnlyKnownKeys() throws IOException {
        Set<String> extra = new TreeSet<>(bundle("ardc-branding").stringPropertyNames());
        extra.removeAll(bundle("raid-custom").stringPropertyNames());
        assertEquals(Set.of(), extra, "a key defined only in ardc-branding has no neutral default");
    }

    @Test
    @DisplayName("raid-custom's defaults name no ARDC address or wording")
    void neutralDefaultsAreNeutral() throws IOException {
        Pattern ardc = Pattern.compile("ARDC|ardc\\.edu\\.au|Australian Research Data Commons", Pattern.CASE_INSENSITIVE);
        List<String> offenders = new ArrayList<>();
        Properties neutral = bundle("raid-custom");
        for (String key : neutral.stringPropertyNames()) {
            if (ardc.matcher(neutral.getProperty(key)).find()) {
                offenders.add(key);
            }
        }
        offenders.sort(String::compareTo);
        assertEquals(List.of(), offenders, "raid-custom message defaults that mention ARDC");
    }

    @Test
    @DisplayName("templates hold no visible text of their own")
    void noLiteralVisibleText() throws IOException {
        List<String> literals = new ArrayList<>();
        for (String theme : THEME_NAMES) {
            for (Path template : templates(theme)) {
                String body = withoutComments(Files.readString(template, StandardCharsets.UTF_8))
                        .replaceAll("(?s)<script\\b.*?</script>", "")
                        .replaceAll("(?s)<style\\b.*?</style>", "")
                        .replaceAll("(?s)</?#[^>]*>", "")
                        .replaceAll("(?s)</?@[^>]*>", "")
                        .replaceAll("(?s)\\$\\{.*?}", "")
                        .replaceAll("(?s)<[^>]*>", "\n");
                for (String line : body.split("\n")) {
                    String text = line.trim();
                    if (!text.isEmpty()) {
                        literals.add(theme + "/" + template.getFileName() + ": " + text);
                    }
                }
            }
        }
        assertEquals(List.of(), literals, "visible text typed into a template");
    }

    @Test
    @DisplayName("templates hold no literal alt, aria-label, title or data-host text")
    void noLiteralAttributeText() throws IOException {
        Pattern attribute = Pattern.compile("\\b(alt|aria-label|title|placeholder|data-host)=\"([^\"$]+)\"");
        List<String> literals = new ArrayList<>();
        for (String theme : THEME_NAMES) {
            for (Path template : templates(theme)) {
                Matcher matcher = attribute.matcher(withoutComments(Files.readString(template, StandardCharsets.UTF_8)));
                while (matcher.find()) {
                    literals.add(theme + "/" + template.getFileName() + ": " + matcher.group(1) + "=\"" + matcher.group(2) + "\"");
                }
            }
        }
        assertEquals(List.of(), literals, "attribute text typed into a template");
    }

    @Test
    @DisplayName("templates hold no link address of their own, apart from the ARDC component scripts")
    void noLiteralLinks() throws IOException {
        // "mailto:${supportEmail}" is a link built from a key, so it is fine.
        Pattern address = Pattern.compile("(https?://|mailto:)(?!\\$\\{)[^\"'\\s<>]+");
        List<String> literals = new ArrayList<>();
        for (String theme : THEME_NAMES) {
            for (Path template : templates(theme)) {
                String body = withoutComments(Files.readString(template, StandardCharsets.UTF_8));
                Matcher matcher = address.matcher(body);
                while (matcher.find()) {
                    String found = matcher.group();
                    // The hosted web component loaders are code, not content.
                    if (found.endsWith("/megamenu.min.js") || found.endsWith("/footer.min.js")) {
                        continue;
                    }
                    // An SVG's xmlns is an XML namespace name, not a link.
                    if (found.startsWith("http://www.w3.org/")) {
                        continue;
                    }
                    literals.add(theme + "/" + template.getFileName() + ": " + found);
                }
            }
        }
        assertEquals(List.of(), literals, "link addresses typed into a template");
    }
}
