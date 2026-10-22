package com.tugba.radyoloji_raporlama_asistani.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SentenceBuilderTest {

    @Test
    void joinWithAnd_handlesOneTwoAndThreeItems() {
        assertThat(SentenceBuilder.joinWithAnd(List.of("a"))).isEqualTo("a");
        assertThat(SentenceBuilder.joinWithAnd(List.of("a", "b"))).isEqualTo("a ve b");
        assertThat(SentenceBuilder.joinWithAnd(List.of("a", "b", "c"))).isEqualTo("a, b ve c");
    }

    @Test
    void capitalizeFirst_usesTurkishRules() {
        assertThat(SentenceBuilder.capitalizeFirst("ileri")).isEqualTo("İleri");
        assertThat(SentenceBuilder.capitalizeFirst("ışık")).isEqualTo("Işık");
    }

    @Test
    void buildSentence_combinesTermsAndPredicate() {
        String sentence = SentenceBuilder.buildSentence(
                List.of("ekstra-aksiyel koleksiyon", "orta hat şifti"), "izlenmemiştir");

        assertThat(sentence).isEqualTo("Ekstra-aksiyel koleksiyon ve orta hat şifti izlenmemiştir.");
    }

    @Test
    void buildFindings_putsAbnormalFirstThenPredicateSentencesInOrder() {
        String text = """
                Olağan Dışı Bulgular: sağ frontalde hiperdens alan

                İzlenmemiştir: ekstra-aksiyel koleksiyon, orta hat şifti

                Saptanmamıştır:

                Açıktır:

                Doğaldır: ventriküler sistem
                """;
        Map<FindingsHeading, String> sections = FindingsParser.parse(text);

        String findings = SentenceBuilder.buildFindings(sections, sections.get(FindingsHeading.OLAGAN_DISI));

        assertThat(findings).isEqualTo(
                "sağ frontalde hiperdens alan\n"
                        + "Ekstra-aksiyel koleksiyon ve orta hat şifti izlenmemiştir.\n"
                        + "Ventriküler sistem doğaldır.");
    }

    @Test
    void buildFindings_skipsBlankAbnormalText() {
        Map<FindingsHeading, String> sections = FindingsParser.parse("Doğaldır: ventriküler sistem");

        String findings = SentenceBuilder.buildFindings(sections, "");

        assertThat(findings).isEqualTo("Ventriküler sistem doğaldır.");
    }

    @Test
    void buildFindings_ordersByHeadingNotByInputOrder() {
        Map<FindingsHeading, String> sections = FindingsParser.parse("Doğaldır: a\n\nİzlenmemiştir: b");

        String findings = SentenceBuilder.buildFindings(sections, "");

        assertThat(findings).isEqualTo("B izlenmemiştir.\nA doğaldır.");
    }
}