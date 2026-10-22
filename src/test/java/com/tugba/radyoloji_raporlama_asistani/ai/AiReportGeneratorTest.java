package com.tugba.radyoloji_raporlama_asistani.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.lenient;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

@ExtendWith(MockitoExtension.class)
class AiReportGeneratorTest {

    private static final String FINDINGS_INPUT = """
            Olağan Dışı Bulgular: sağ frontalde hemoraji

            Doğaldır: ventriküler sistem
            """;

    @Mock
    private ResultCategoryRepository resultCategoryRepository;

    @BeforeEach
    void setUp() {
        lenient().when(resultCategoryRepository.findAll()).thenReturn(List.of(
                category("Normal Bulgu", "İncelemede akut patolojik bulgu saptanmamıştır.", UrgencyLevel.RUTIN),
                category("Akut Kanama", "Acil klinik değerlendirme önerilir.", UrgencyLevel.KRITIK),
                category("Pnömotoraks", "Acil klinik değerlendirme önerilir.", UrgencyLevel.KRITIK),
                category("Kırık", "Ortopedi değerlendirmesi önerilir.", UrgencyLevel.ACIL),
                category("İleri İnceleme Önerisi", "İleri görüntüleme ve klinik korelasyon önerilir.",
                        UrgencyLevel.RUTIN)));
    }

    @Test
    void generate_rewritesTextsAndAppendsCategorySentence() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Sağ frontalde hemoraji izlenmektedir.",
                "Sağ frontalde hemoraji şüphesi.",
                List.of("Akut Kanama")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "sağ frontalde hemoraji şüphesi");

        assertThat(result.findings())
                .isEqualTo("Sağ frontalde hemoraji izlenmektedir.\nVentriküler sistem doğaldır.");
        assertThat(result.result())
                .isEqualTo("Sağ frontalde hemoraji şüphesi. Acil klinik değerlendirme önerilir.");
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.KRITIK);
        assertThat(result.resultCategories()).extracting(ResultCategory::getCategoryName)
                .containsExactly("Akut Kanama");
        assertThat(result.aiAssisted()).isTrue();
    }

    @Test
    void generate_ignoresCategoryNamesNotInDatabase() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Sağ frontalde hemoraji.", "Hemoraji şüphesi.", List.of("Akut Kanama", "Uydurma Kategori")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "hemoraji şüphesi");

        assertThat(result.resultCategories()).extracting(ResultCategory::getCategoryName)
                .containsExactly("Akut Kanama");
    }

    @Test
    void generate_neverSelectsNormalCategory() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Sağ frontalde hemoraji.", "Hemoraji şüphesi.", List.of("Normal Bulgu")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "hemoraji şüphesi");

        assertThat(result.resultCategories()).isEmpty();
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.BELIRSIZ);
    }

    @Test
    void generate_addsSameSentenceOnlyOnce() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Sağ frontalde hemoraji.", "Kanama ve pnömotoraks şüphesi.", List.of("Akut Kanama", "Pnömotoraks")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "kanama ve pnömotoraks şüphesi");

        assertThat(result.result())
                .isEqualTo("Kanama ve pnömotoraks şüphesi. Acil klinik değerlendirme önerilir.");
    }

    @Test
    void generate_picksMostUrgentLevel() {
        AiReportGenerator mixed = generatorReturning(new AiAssistance(
                "Bulgu.", "Kırık ve kanama.", List.of("Kırık", "Akut Kanama")));
        AiReportGenerator lowerOnly = generatorReturning(new AiAssistance(
                "Bulgu.", "Kırık ve ileri inceleme.", List.of("Kırık", "İleri İnceleme Önerisi")));

        assertThat(mixed.generate(FINDINGS_INPUT, "kırık ve kanama").urgencyLevel())
                .isEqualTo(UrgencyLevel.KRITIK);
        assertThat(lowerOnly.generate(FINDINGS_INPUT, "kırık ve ileri inceleme").urgencyLevel())
                .isEqualTo(UrgencyLevel.ACIL);
    }

    @Test
    void generate_fallsBackToDoctorTextWhenModelFails() {
        AiAssistant failing = (abnormal, result, categories) -> {
            throw new IllegalStateException("bağlantı hatası");
        };
        AiReportGenerator generator = new AiReportGenerator(failing, resultCategoryRepository);

        GenerationResult result = generator.generate(FINDINGS_INPUT, "sağ frontalde hemoraji şüphesi");

        assertThat(result.findings()).isEqualTo("sağ frontalde hemoraji\nVentriküler sistem doğaldır.");
        assertThat(result.result()).isEqualTo("sağ frontalde hemoraji şüphesi");
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.BELIRSIZ);
        assertThat(result.resultCategories()).isEmpty();
        assertThat(result.aiAssisted()).isFalse();
    }

    @Test
    void generate_doesNotCallModelWhenAbnormalFindingsAndResultAreEmpty() {
        AiAssistant mustNotBeCalled = (abnormal, result, categories) -> fail("Model çağrılmamalıydı");
        AiReportGenerator generator = new AiReportGenerator(mustNotBeCalled, resultCategoryRepository);

        GenerationResult result = generator.generate("Doğaldır: ventriküler sistem", "");

        assertThat(result.findings()).isEqualTo("Ventriküler sistem doğaldır.");
        assertThat(result.result()).isEmpty();
        assertThat(result.urgencyLevel()).isEqualTo(UrgencyLevel.BELIRSIZ);
        assertThat(result.aiAssisted()).isFalse();
    }

    @Test
    void generate_doesNotLetModelInventTextForEmptyFields() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Uydurma bulgu.", "Kırık şüphesi.", List.of("Kırık")));

        GenerationResult result = generator.generate("Doğaldır: ventriküler sistem", "kırık şüphesi");

        assertThat(result.findings()).isEqualTo("Ventriküler sistem doğaldır.");
        assertThat(result.result()).isEqualTo("Kırık şüphesi. Ortopedi değerlendirmesi önerilir.");
    }

    @Test
    void generate_doesNotRepeatSentenceAlreadyInResultText() {
        AiReportGenerator generator = generatorReturning(new AiAssistance(
                "Bulgu.", "Kırık şüphesi. Ortopedi değerlendirmesi önerilir.", List.of("Kırık")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "kırık şüphesi, ortopedi değerlendirmesi önerilir");

        assertThat(result.result()).isEqualTo("Kırık şüphesi. Ortopedi değerlendirmesi önerilir.");
    }

    @Test
    void generate_keepsDoctorTextAndAddsFinalPeriodWhenModelReturnsBlank() {
        AiReportGenerator generator = generatorReturning(new AiAssistance("", "", List.of("Kırık")));

        GenerationResult result = generator.generate(FINDINGS_INPUT, "kırık şüphesi");

        assertThat(result.findings()).isEqualTo("sağ frontalde hemoraji\nVentriküler sistem doğaldır.");
        assertThat(result.result()).isEqualTo("kırık şüphesi. Ortopedi değerlendirmesi önerilir.");
    }

    private AiReportGenerator generatorReturning(AiAssistance assistance) {
        AiAssistant assistant = (abnormal, result, categories) -> assistance;
        return new AiReportGenerator(assistant, resultCategoryRepository);
    }

    private ResultCategory category(String name, String sentence, UrgencyLevel urgency) {
        ResultCategory category = new ResultCategory();
        category.setCategoryName(name);
        category.setApprovedSentence(sentence);
        category.setAssociatedUrgencyLevel(urgency);
        return category;
    }
}