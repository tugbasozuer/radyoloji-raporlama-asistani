package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tugba.radyoloji_raporlama_asistani.entity.Exam;
import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TemplateReportGenerator implements ReportGenerator {

    private static final String NORMAL_CATEGORY = "Normal Bulgu";

    private final ResultCategoryRepository resultCategoryRepository;

    @Override
    public GenerationResult generate(Exam exam,
            String findingsInput,
            String resultInput,
            boolean normalExam) {

        Map<FindingsHeading, String> sections = FindingsParser.parse(findingsInput);
        String findings = SentenceBuilder.buildFindings(
                sections, sections.get(FindingsHeading.OLAGAN_DISI));

        if (normalExam) {
            ResultCategory normal = resultCategoryRepository.findByCategoryName(NORMAL_CATEGORY)
                    .orElseThrow(() -> new IllegalStateException(
                            "Sonuç kategorisi bulunamadı: " + NORMAL_CATEGORY));
            Set<ResultCategory> categories = new HashSet<>();
            categories.add(normal);
            return new GenerationResult(findings, normal.getApprovedSentence(),
                    normal.getAssociatedUrgencyLevel(), categories, false);
        }

        return new GenerationResult(findings, "", UrgencyLevel.BELIRSIZ, new HashSet<>(), false);
    }
}