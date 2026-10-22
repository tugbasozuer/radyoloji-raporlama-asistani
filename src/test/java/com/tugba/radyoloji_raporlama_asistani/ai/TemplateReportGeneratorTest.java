package com.tugba.radyoloji_raporlama_asistani.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tugba.radyoloji_raporlama_asistani.entity.Exam;
import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

@ExtendWith(MockitoExtension.class)
class TemplateReportGeneratorTest {

    @Mock
    private ResultCategoryRepository resultCategoryRepository;

    @InjectMocks
    private TemplateReportGenerator generator;

    @Test
    void generate_normalExam_usesNormalCategorySentenceAndRoutineUrgency() {
        ResultCategory normal = new ResultCategory();
        normal.setCategoryName("Normal Bulgu");
        normal.setApprovedSentence("İncelemede akut patolojik bulgu saptanmamıştır.");
        normal.setAssociatedUrgencyLevel(UrgencyLevel.RUTIN);
        when(resultCategoryRepository.findByCategoryName("Normal Bulgu")).thenReturn(Optional.of(normal));

        GenerationResult result = generator.generate(new Exam(), "Doğaldır: ventriküler sistem", "", true);

        assertThat(result.findings()).isEqualTo("Ventriküler sistem doğaldır.");
        assertThat(result.result()).isEqualTo("İncelemede akut patolojik bulgu saptanmamıştır.");
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.RUTIN);
        assertThat(result.resultCategories()).containsExactly(normal);
        assertThat(result.aiAssisted()).isFalse();
    }

    @Test
    void generate_abnormalExam_leavesResultEmptyAndUrgencyUndetermined() {
        GenerationResult result = generator.generate(new Exam(), "Olağan Dışı Bulgular: kitle", "hemoraji şüphesi", false);

        assertThat(result.findings()).isEqualTo("kitle");
        assertThat(result.result()).isEmpty();
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.BELIRSIZ);
        assertThat(result.resultCategories()).isEmpty();
        assertThat(result.aiAssisted()).isFalse();
        verifyNoInteractions(resultCategoryRepository);
    }

    @Test
    void generate_normalExam_throwsWhenNormalCategoryIsMissing() {
        when(resultCategoryRepository.findByCategoryName("Normal Bulgu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> generator.generate(new Exam(), "", "", true))
                .isInstanceOf(IllegalStateException.class);
    }
}