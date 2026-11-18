# Test notu kümesi etiketleme kuralları

Dosya: `test_notlari.csv` (UTF-8, ayraç `;`, kategori ve bilgi öğesi listeleri `|` ile ayrılır).

Sütunlar: `id`, `grup`, `olagan_disi_not`, `sonuc_notu`, `dogru_kategoriler`, `dogru_aciliyet`, `bilgi_ogeleri`.

Tüm notlar kurgusaldır. Gerçek hasta raporlarından veya HBYS verisinden alınmamıştır.

## Kategori etiketi

- Etiket yalnızca `sonuc_notu` için verilir. Kategori adları V5 seed'indeki 9 kategoridir ("Normal Bulgu" listede yoktur).
- Notta bulgu **var** olarak belirtilmişse kategori seçilir. "Şüphesi", "olası", "ile uyumlu" gibi kesinlik ifadeleri kategoriyi değiştirmez.
- Bulgu olumsuzlanmışsa ("izlenmemiştir", "yoktur", "saptanmamıştır") o kategori seçilmez.
- Bulgu hiçbir kategoriye uymuyorsa boş bırakılır.
- "İleri İnceleme Önerisi" yalnızca notta ileri görüntüleme veya kontrol görüntüleme önerildiğinde seçilir. "Klinik korelasyon" tek başına bu kategori sayılmaz.

## Aciliyet etiketi

Seçilen kategorilerin en yükseği (KRITIK > ACIL > RUTIN). Kategori yoksa `BELIRSIZ`. Bu kural uygulamadaki `AiReportGenerator` ile aynıdır.

## Gruplar

- `dogrudan`: terim listedeki bir terimin kökünü içerir ve bulgu olumludur.
- `es_anlamli`: kategori anlamca vardır ama listedeki terimlerden hiçbiri geçmez.
- `negatif`: terim geçer ama bulgu olumsuzlanmıştır. Olumsuzlama ile gerçek bir bulgunun birlikte geçtiği notlar da bu gruptadır.
- `yazim_hatasi`: terim yanlış yazılmıştır.
- `coklu`: birden fazla kategori vardır.
- `uymayan`: bulgu vardır ama 9 kategoriden hiçbirine uymaz.

## Bilgi öğeleri (Tablo 3 paydası)

`olagan_disi_not` ve `sonuc_notu` birlikte okunur. `yan` (sağ, sol, bilateral), `olcu` (mm, cm, % gibi sayısal ölçü), `negatif` (bir bulgunun yokluğunu belirten ifade), `kesinlik` ("şüphesi", "olası", "ile uyumlu", "düşündürmektedir" gibi). Hiçbiri yoksa `-`.
