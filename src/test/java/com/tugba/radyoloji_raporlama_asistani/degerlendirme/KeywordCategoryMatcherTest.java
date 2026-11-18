package com.tugba.radyoloji_raporlama_asistani.degerlendirme;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;

class KeywordCategoryMatcherTest {

    private static final Map<String, UrgencyLevel> URGENCY = Map.of(
            "Akut Kanama", UrgencyLevel.KRITIK,
            "Akut İskemi", UrgencyLevel.KRITIK,
            "Kırık", UrgencyLevel.ACIL,
            "İleri İnceleme Önerisi", UrgencyLevel.RUTIN);

    @Test
    void match_findsCategoryEvenWithSuffix() {
        assertThat(KeywordCategoryMatcher.match("Sağ parietalde akut kanamaya ait bulgular."))
                .containsExactly("Akut Kanama");
    }

    @Test
    void match_usesTurkishLowercase() {
        assertThat(KeywordCategoryMatcher.match("PNÖMOTORAKS izlenmiştir.")).containsExactly("Pnömotoraks");
        assertThat(KeywordCategoryMatcher.match("KIRIK hattı izlenmiştir.")).containsExactly("Kırık");
        assertThat(KeywordCategoryMatcher.match("AKUT İSKEMİ lehine.")).containsExactly("Akut İskemi");
    }

    @Test
    void match_doesNotUnderstandNegation_byDesign() {
        // Temel yontemin bilinen siniri: "izlenmemistir" olumsuzlugunu gormez.
        assertThat(KeywordCategoryMatcher.match("Kanama izlenmemiştir.")).containsExactly("Akut Kanama");
    }

    @Test
    void match_returnsEmptyWhenNothingMatchesOrTextIsBlank() {
        assertThat(KeywordCategoryMatcher.match("Belirgin patoloji yoktur.")).isEmpty();
        assertThat(KeywordCategoryMatcher.match("  ")).isEmpty();
        assertThat(KeywordCategoryMatcher.match(null)).isEmpty();
    }

    @Test
    void match_returnsAllMatchedCategoriesInListOrder() {
        assertThat(KeywordCategoryMatcher.match("Fraktür ve akut iskemi, kontrol MR önerilir."))
                .containsExactly("Akut İskemi", "Kırık", "İleri İnceleme Önerisi");
    }

    @Test
    void highestUrgency_picksMostUrgentOrBelirsizWhenEmpty() {
        assertThat(KeywordCategoryMatcher.highestUrgency(List.of("Kırık", "Akut İskemi"), URGENCY))
                .isEqualTo(UrgencyLevel.KRITIK);
        assertThat(KeywordCategoryMatcher.highestUrgency(List.of("Kırık", "İleri İnceleme Önerisi"), URGENCY))
                .isEqualTo(UrgencyLevel.ACIL);
        assertThat(KeywordCategoryMatcher.highestUrgency(List.of(), URGENCY)).isEqualTo(UrgencyLevel.BELIRSIZ);
    }

    @Test
    void termCount_is25() {
        assertThat(KeywordCategoryMatcher.termCount()).isEqualTo(25);
    }
}
