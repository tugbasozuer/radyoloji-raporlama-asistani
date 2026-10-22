package com.tugba.radyoloji_raporlama_asistani.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class FindingsParserTest {

    @Test
    void parse_splitsTextIntoSectionsByHeading() {
        String text = """
                Olağan Dışı Bulgular: sağ frontalde hiperdens alan

                İzlenmemiştir: ekstra-aksiyel koleksiyon, orta hat şifti

                Saptanmamıştır:

                Açıktır:

                Doğaldır: ventriküler sistem
                """;

        Map<FindingsHeading, String> sections = FindingsParser.parse(text);

        assertThat(sections.get(FindingsHeading.OLAGAN_DISI)).isEqualTo("sağ frontalde hiperdens alan");
        assertThat(sections.get(FindingsHeading.IZLENMEMISTIR))
                .isEqualTo("ekstra-aksiyel koleksiyon, orta hat şifti");
        assertThat(sections.get(FindingsHeading.SAPTANMAMISTIR)).isEmpty();
        assertThat(sections.get(FindingsHeading.ACIKTIR)).isEmpty();
        assertThat(sections.get(FindingsHeading.DOGALDIR)).isEqualTo("ventriküler sistem");
    }

    @Test
    void parse_recognizesHeadingsWithoutTurkishCharacters() {
        Map<FindingsHeading, String> sections = FindingsParser.parse("Izlenmemistir: kitle");

        assertThat(sections.get(FindingsHeading.IZLENMEMISTIR)).isEqualTo("kitle");
    }

    @Test
    void parse_putsTextBeforeFirstHeadingIntoAbnormalSection() {
        Map<FindingsHeading, String> sections = FindingsParser.parse("kitle lezyonu\n\nDoğaldır: ventriküler sistem");

        assertThat(sections.get(FindingsHeading.OLAGAN_DISI)).isEqualTo("kitle lezyonu");
        assertThat(sections.get(FindingsHeading.DOGALDIR)).isEqualTo("ventriküler sistem");
    }

    @Test
    void parse_treatsUnknownColonLineAsContent() {
        Map<FindingsHeading, String> sections = FindingsParser
                .parse("Olağan Dışı Bulgular: lezyon\nNot: kontrol önerilir");

        assertThat(sections.get(FindingsHeading.OLAGAN_DISI)).isEqualTo("lezyon\nNot: kontrol önerilir");
    }

    @Test
    void parse_handlesNullText() {
        Map<FindingsHeading, String> sections = FindingsParser.parse(null);

        assertThat(sections.get(FindingsHeading.OLAGAN_DISI)).isEmpty();
        assertThat(sections.get(FindingsHeading.DOGALDIR)).isEmpty();
    }

    @Test
    void parseTerms_splitsOnCommaAndSemicolonLowercasesAndRemovesDuplicates() {
        List<String> terms = FindingsParser.parseTerms("Ekstra-aksiyel koleksiyon, Orta hat şifti; ORTA HAT ŞİFTİ");

        assertThat(terms).containsExactly("ekstra-aksiyel koleksiyon", "orta hat şifti");
    }

    @Test
    void parseTerms_returnsEmptyListForBlankOrNull() {
        assertThat(FindingsParser.parseTerms("")).isEmpty();
        assertThat(FindingsParser.parseTerms(null)).isEmpty();
    }
}