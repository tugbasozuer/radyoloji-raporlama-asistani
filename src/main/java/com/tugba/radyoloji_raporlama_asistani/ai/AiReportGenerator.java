package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportGenerator {

    private static final String NORMAL_CATEGORY = "Normal Bulgu";

    private final AiAssistant aiAssistant;
    private final ResultCategoryRepository resultCategoryRepository;

    public GenerationResult generate(String findingsInput, String resultInput) {
        Map<FindingsHeading, String> sections = FindingsParser.parse(findingsInput);
        String abnormal = sections.get(FindingsHeading.OLAGAN_DISI);
        String doctorResult = resultInput == null ? "" : resultInput.trim();

        GenerationResult fallback = new GenerationResult(
                SentenceBuilder.buildFindings(sections, abnormal),
                doctorResult,
                UrgencyLevel.BELIRSIZ,
                new HashSet<>(),
                false);

        if (abnormal.isBlank() && doctorResult.isEmpty()) {
            return fallback;
        }

        List<ResultCategory> selectable = resultCategoryRepository.findAll().stream()
                .filter(category -> !NORMAL_CATEGORY.equals(category.getCategoryName()))
                .toList();
        Map<String, String> categoryMap = new LinkedHashMap<>();
        selectable.forEach(category -> categoryMap.put(category.getCategoryName(), category.getApprovedSentence()));

        AiAssistance assistance;
        try {
            assistance = aiAssistant.assist(abnormal, doctorResult, categoryMap);
        } catch (RuntimeException e) {
            log.warn("Yapay zeka çağrısı başarısız oldu, yedek davranış uygulanıyor: {}", e.toString());
            return fallback;
        }
        if (assistance == null) {
            return fallback;
        }

        String findings = SentenceBuilder.buildFindings(sections, chooseText(assistance.findings(), abnormal));
        String resultText = chooseText(assistance.result(), doctorResult);
        List<ResultCategory> matched = doctorResult.isEmpty()
                ? List.of()
                : matchCategories(assistance.categories(), selectable);

        Set<String> sentences = new LinkedHashSet<>();
        for (ResultCategory category : matched) {
            if (!resultText.contains(category.getApprovedSentence())) {
                sentences.add(category.getApprovedSentence());
            }
        }
        List<String> parts = new ArrayList<>();
        if (!resultText.isEmpty()) {
            parts.add(sentences.isEmpty() ? resultText : withFinalPunctuation(resultText));
        }
        parts.addAll(sentences);

        UrgencyLevel urgency = matched.stream()
                .map(ResultCategory::getAssociatedUrgencyLevel)
                .min(Comparator.naturalOrder())
                .orElse(UrgencyLevel.BELIRSIZ);

        return new GenerationResult(findings, String.join(" ", parts), urgency, new HashSet<>(matched), true);
    }

    private String withFinalPunctuation(String text) {
        char last = text.charAt(text.length() - 1);
        return (last == '.' || last == '!' || last == '?') ? text : text + ".";
    }

    private String chooseText(String aiText, String original) {
        if (original.isBlank()) {
            return "";
        }
        return (aiText == null || aiText.isBlank()) ? original : aiText.trim();
    }

    private List<ResultCategory> matchCategories(List<String> names, List<ResultCategory> selectable) {
        List<ResultCategory> matched = new ArrayList<>();
        if (names == null) {
            return matched;
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            selectable.stream()
                    .filter(category -> category.getCategoryName().equals(name.trim()))
                    .findFirst()
                    .filter(category -> !matched.contains(category))
                    .ifPresent(matched::add);
        }
        return matched;
    }
}