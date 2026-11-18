package com.tugba.radyoloji_raporlama_asistani.degerlendirme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** test_notlari.csv'deki bir satır. Dosya UTF-8, ayraç ';', liste alanları '|' ile ayrılır. */
record TestNotu(String id, String grup, String olaganDisiNot, String sonucNotu,
        List<String> dogruKategoriler, String dogruAciliyet, List<String> bilgiOgeleri) {

    private static final String YOL = "/degerlendirme/test_notlari.csv";

    static List<TestNotu> oku() throws IOException {
        try (InputStream in = TestNotu.class.getResourceAsStream(YOL)) {
            if (in == null) {
                throw new IOException("Kaynak bulunamadı: " + YOL);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<TestNotu> notlar = new ArrayList<>();
            reader.readLine(); // başlık satırı
            String satir;
            while ((satir = reader.readLine()) != null) {
                if (satir.isBlank()) {
                    continue;
                }
                String[] alan = satir.split(";", -1);
                if (alan.length != 7) {
                    throw new IOException("Beklenen 7 sütun, bulunan " + alan.length + ": " + satir);
                }
                notlar.add(new TestNotu(alan[0], alan[1], alan[2], alan[3], liste(alan[4]), alan[5],
                        alan[6].equals("-") ? List.of() : liste(alan[6])));
            }
            return notlar;
        }
    }

    private static List<String> liste(String ham) {
        return ham.isBlank() ? List.of() : List.of(ham.split("\\|"));
    }
}
