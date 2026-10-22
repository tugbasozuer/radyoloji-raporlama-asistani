package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SentenceBuilder {

    private static final Locale TR = Locale.forLanguageTag("tr");

    private SentenceBuilder() {
    }

    public static String buildFindings(Map<FindingsHeading, String> sections, String abnormalText) {
        List<String> blocks = new ArrayList<>();
        if (abnormalText != null && !abnormalText.isBlank()) {
            blocks.add(abnormalText.trim());
        }
        for (FindingsHeading heading : FindingsHeading.values()) {
            if (!heading.isPredicateHeading()) {
                continue;
            }
            List<String> terms = FindingsParser.parseTerms(sections.get(heading));
            if (!terms.isEmpty()) {
                blocks.add(buildSentence(terms, heading.getPredicate()));
            }
        }
        return String.join("\n", blocks);
    }

    static String buildSentence(List<String> terms, String predicate) {
        return capitalizeFirst(joinWithAnd(terms)) + " " + predicate + ".";
    }

    static String joinWithAnd(List<String> items) {
        if (items.size() == 1) {
            return items.get(0);
        }
        String head = String.join(", ", items.subList(0, items.size() - 1));
        return head + " ve " + items.get(items.size() - 1);
    }

    static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.substring(0, 1).toUpperCase(TR) + text.substring(1);
    }
}