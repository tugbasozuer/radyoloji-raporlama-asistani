package com.tugba.radyoloji_raporlama_asistani.degerlendirme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;

/**
 * Degerlendirme deneyindeki temel (baseline) yontem: sonuc metninde anahtar kelime arar.
 * Olumsuzlamayi ("kanama izlenmemistir") bilerek tanimaz, boylece LLM'in farki gorulebilir.
 * Terim listesi test notlari yazilmadan once sabitlenmistir, notlara gore degistirilmemelidir.
 */
public final class KeywordCategoryMatcher {

    private static final Locale TR = Locale.forLanguageTag("tr");

    private record CategoryTerms(String categoryName, List<String> terms) {
    }

    // Kategori adlari V5 seed'indeki adlarla birebir ayni olmali (LLM sonuclariyla karsilastirilacak).
    private static final List<CategoryTerms> CATEGORY_TERMS = List.of(
            new CategoryTerms("Akut Kanama", List.of("kanama", "hemoraji", "hematom")),
            new CategoryTerms("Akut İskemi", List.of("iskemi", "infarkt")),
            new CategoryTerms("Pnömotoraks", List.of("pnömotoraks", "plevral hava")),
            new CategoryTerms("Pulmoner Emboli", List.of("emboli", "dolum defekti")),
            new CategoryTerms("Serbest Hava", List.of("serbest hava", "perforasyon", "pnömoperitoneum")),
            new CategoryTerms("Kırık", List.of("kırık", "fraktür")),
            new CategoryTerms("Pnömoni", List.of("pnömoni", "konsolidasyon", "zatürre")),
            new CategoryTerms("Barsak Tıkanıklığı", List.of("tıkanıklık", "ileus", "obstrüksiyon")),
            new CategoryTerms("İleri İnceleme Önerisi",
                    List.of("ileri inceleme", "ileri görüntüleme", "kontrol mr", "kontrol bt", "korelasyon")));

    private KeywordCategoryMatcher() {
    }

    /** Sonuc metninde en az bir terimi gecen kategorilerin adlarini listedeki sirayla dondurur. */
    public static List<String> match(String resultText) {
        List<String> matched = new ArrayList<>();
        if (resultText == null || resultText.isBlank()) {
            return matched;
        }
        // Turkce yerel ayar sart: "KIRIK" -> "kırık", "İSKEMİ" -> "iskemi" (varsayilan ayarda i/ı bozulur).
        String text = resultText.toLowerCase(TR);
        for (CategoryTerms category : CATEGORY_TERMS) {
            boolean found = category.terms().stream().anyMatch(text::contains);
            if (found) {
                matched.add(category.categoryName());
            }
        }
        return matched;
    }

    /** Secilen kategorilerin en yuksek aciliyetini verir. Hicbiri secilmediyse BELIRSIZ doner. */
    public static UrgencyLevel highestUrgency(Collection<String> categories,
            Map<String, UrgencyLevel> urgencyByCategory) {
        return categories.stream()
                .map(urgencyByCategory::get)
                .min(Comparator.naturalOrder())
                .orElse(UrgencyLevel.BELIRSIZ);
    }

    /** Listedeki toplam terim sayisi (raporda [k] olarak gecer). */
    public static int termCount() {
        return CATEGORY_TERMS.stream().mapToInt(category -> category.terms().size()).sum();
    }
}
