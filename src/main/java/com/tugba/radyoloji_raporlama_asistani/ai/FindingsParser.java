package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FindingsParser {

    private static final Locale TR = Locale.forLanguageTag("tr");
    private static final Pattern HEADING_LINE = Pattern.compile("^\\s*([^:]{1,40}):(.*)$");

    private FindingsParser() {
    }

    public static Map<FindingsHeading, String> parse(String text) {
        Map<FindingsHeading, List<String>> parts = new EnumMap<>(FindingsHeading.class);
        for (FindingsHeading heading : FindingsHeading.values()) {
            parts.put(heading, new ArrayList<>());
        }

        FindingsHeading current = FindingsHeading.OLAGAN_DISI;
        String safeText = text == null ? "" : text;
        for (String line : safeText.split("\\R")) {
            Matcher matcher = HEADING_LINE.matcher(line);
            FindingsHeading heading = matcher.matches() ? findHeading(matcher.group(1)) : null;
            if (heading != null) {
                current = heading;
                String rest = matcher.group(2).trim();
                if (!rest.isEmpty()) {
                    parts.get(current).add(rest);
                }
            } else if (!line.isBlank()) {
                parts.get(current).add(line.trim());
            }
        }

        Map<FindingsHeading, String> sections = new EnumMap<>(FindingsHeading.class);
        parts.forEach((heading, lines) -> sections.put(heading, String.join("\n", lines)));
        return sections;
    }

    public static String canonicalText(String text) {
        Map<FindingsHeading, String> sections = parse(text);
        List<String> blocks = new ArrayList<>();
        for (FindingsHeading heading : FindingsHeading.values()) {
            blocks.add(heading.getLabel() + ": " + sections.get(heading));
        }
        return String.join("\n\n", blocks);
    }

    public static List<String> parseTerms(String raw) {
        if (raw == null) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        for (String part : raw.split("[,;\\n]")) {
            String term = part.trim().toLowerCase(TR);
            if (!term.isEmpty()) {
                terms.add(term);
            }
        }
        return new ArrayList<>(terms);
    }

    private static FindingsHeading findHeading(String name) {
        String folded = fold(name);
        for (FindingsHeading heading : FindingsHeading.values()) {
            if (heading.getAliases().contains(folded)) {
                return heading;
            }
        }
        return null;
    }

    private static String fold(String text) {
        return text.toLowerCase(TR)
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                .replaceAll("\\s+", " ").trim();
    }
}