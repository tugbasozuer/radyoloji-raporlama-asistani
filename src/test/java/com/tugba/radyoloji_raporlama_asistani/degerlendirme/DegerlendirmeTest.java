package com.tugba.radyoloji_raporlama_asistani.degerlendirme;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.tugba.radyoloji_raporlama_asistani.ai.AiAssistance;
import com.tugba.radyoloji_raporlama_asistani.ai.AiAssistant;
import com.tugba.radyoloji_raporlama_asistani.ai.AiReportGenerator;
import com.tugba.radyoloji_raporlama_asistani.ai.GenerationResult;
import com.tugba.radyoloji_raporlama_asistani.entity.ResultCategory;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UrgencyLevel;
import com.tugba.radyoloji_raporlama_asistani.repository.ResultCategoryRepository;

/**
 * Değerlendirme deneyi. Her test notu için LLM'li yolu 3 kez, anahtar kelime yöntemini 1 kez çalıştırır ve
 * ham sonuçları target/degerlendirme/ altına CSV olarak yazar. Hiçbir metrik burada hesaplanmaz.
 *
 * Gerçek API çağrısı yaptığı için varsayılan "mvn test" ve CI dışındadır. Çalıştırma:
 *   ANTHROPIC_API_KEY=... ./mvnw test -Pdegerlendirme
 */
@Tag("degerlendirme")
@SpringBootTest
@Import(DegerlendirmeVeritabani.class)
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class DegerlendirmeTest {

    private static final int CALISMA_SAYISI = 3;
    private static final String NORMAL_KATEGORI = "Normal Bulgu";
    private static final Path CIKTI = Path.of("target", "degerlendirme");

    @Autowired
    private AiAssistant aiAssistant;

    @Autowired
    private ResultCategoryRepository kategoriDeposu;

    @Value("${spring.ai.anthropic.chat.model}")
    private String modelAdi;

    @Test
    void degerlendirmeyiCalistir() throws IOException {
        List<TestNotu> notlar = TestNotu.oku();
        List<ResultCategory> kategoriler = kategoriDeposu.findAll();
        Set<String> secilebilirAdlar = kategoriler.stream()
                .map(ResultCategory::getCategoryName)
                .filter(ad -> !NORMAL_KATEGORI.equals(ad))
                .collect(Collectors.toSet());
        Map<String, UrgencyLevel> aciliyetByAd = kategoriler.stream()
                .collect(Collectors.toMap(ResultCategory::getCategoryName, ResultCategory::getAssociatedUrgencyLevel));

        // Etiket dosyasındaki bir yazım hatası sonuçları sessizce bozmasın.
        for (TestNotu not : notlar) {
            assertThat(secilebilirAdlar).containsAll(not.dogruKategoriler());
        }

        Files.createDirectories(CIKTI);
        baselineCalistir(notlar, aciliyetByAd);
        llmCalistir(notlar, secilebilirAdlar);
        bilgiDosyasiYaz(notlar.size());
    }

    private void baselineCalistir(List<TestNotu> notlar, Map<String, UrgencyLevel> aciliyetByAd) throws IOException {
        // JIT ısınması: ölçülen süreler ilk çağrıların yavaşlığını yansıtmasın.
        for (int i = 0; i < 2000; i++) {
            KeywordCategoryMatcher.match(notlar.get(i % notlar.size()).sonucNotu());
        }
        List<String> satirlar = new ArrayList<>();
        satirlar.add("not_id;secilen_kategoriler;secilen_aciliyet;sure_us");
        for (TestNotu not : notlar) {
            long baslangic = System.nanoTime();
            List<String> secilen = KeywordCategoryMatcher.match(not.sonucNotu());
            UrgencyLevel aciliyet = KeywordCategoryMatcher.highestUrgency(secilen, aciliyetByAd);
            double mikrosaniye = (System.nanoTime() - baslangic) / 1_000.0;
            satirlar.add(String.join(";", not.id(), csv(String.join("|", secilen)), aciliyet.name(),
                    sayi(mikrosaniye)));
        }
        Files.write(CIKTI.resolve("baseline.csv"), satirlar, StandardCharsets.UTF_8);
    }

    private void llmCalistir(List<TestNotu> notlar, Set<String> secilebilirAdlar) throws IOException {
        RecordingAiAssistant kaydedici = new RecordingAiAssistant(aiAssistant);
        AiReportGenerator uretici = new AiReportGenerator(kaydedici, kategoriDeposu);

        List<String> satirlar = new ArrayList<>();
        satirlar.add("not_id;calisma;secilen_kategoriler;secilen_aciliyet;ham_kategoriler;listede_olmayan;"
                + "sure_ms;ai_destekli;hata;ham_findings;ham_result;nihai_sonuc");

        // Çalışmalar iç içe değil art arda: gün içi servis yoğunluğu 3 çalışmaya dengeli dağılsın.
        for (int calisma = 1; calisma <= CALISMA_SAYISI; calisma++) {
            for (TestNotu not : notlar) {
                GenerationResult sonuc = uretici.generate(not.olaganDisiNot(), not.sonucNotu());
                AiAssistance ham = kaydedici.lastRaw();

                List<String> secilen = sonuc.resultCategories().stream()
                        .map(ResultCategory::getCategoryName).sorted().toList();
                List<String> hamKategoriler = ham == null || ham.categories() == null ? List.of() : ham.categories();
                long listedeOlmayan = hamKategoriler.stream()
                        .filter(ad -> ad == null || !secilebilirAdlar.contains(ad.trim())).count();

                satirlar.add(String.join(";",
                        not.id(),
                        String.valueOf(calisma),
                        csv(String.join("|", secilen)),
                        sonuc.urgencyLevel().name(),
                        csv(String.join("|", hamKategoriler)),
                        String.valueOf(listedeOlmayan),
                        sayi(kaydedici.lastMillis()),
                        String.valueOf(sonuc.aiAssisted()),
                        csv(kaydedici.lastError()),
                        csv(ham == null ? "" : ham.findings()),
                        csv(ham == null ? "" : ham.result()),
                        csv(sonuc.result())));
            }
            System.out.printf("Çalışma %d/%d tamamlandı.%n", calisma, CALISMA_SAYISI);
        }
        Files.write(CIKTI.resolve("runs.csv"), satirlar, StandardCharsets.UTF_8);
    }

    private void bilgiDosyasiYaz(int notSayisi) throws IOException {
        List<String> satirlar = List.of(
                "tarih=" + OffsetDateTime.now(),
                "model=" + modelAdi,
                "java=" + System.getProperty("java.version"),
                "isletim_sistemi=" + System.getProperty("os.name") + " " + System.getProperty("os.version"),
                "not_sayisi=" + notSayisi,
                "calisma_sayisi=" + CALISMA_SAYISI,
                "anahtar_kelime_terim_sayisi=" + KeywordCategoryMatcher.termCount());
        Files.write(CIKTI.resolve("calisma_bilgisi.txt"), satirlar, StandardCharsets.UTF_8);
    }

    /** ';', tırnak veya satır sonu içeren alanları RFC 4180'e uygun şekilde tırnak içine alır. */
    private static String csv(String deger) {
        if (deger == null) {
            return "";
        }
        if (deger.contains(";") || deger.contains("\"") || deger.contains("\n") || deger.contains("\r")) {
            return "\"" + deger.replace("\"", "\"\"") + "\"";
        }
        return deger;
    }

    private static String sayi(double deger) {
        return String.format(Locale.ROOT, "%.3f", deger);
    }
}
