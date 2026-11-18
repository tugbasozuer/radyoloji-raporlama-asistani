# Değerlendirme deneyi

Bu klasör, raporun 5.1 ve 5.2 bölümlerindeki sayıların (Tablo 2, Tablo 3, Şekil 2) nasıl üretildiğini gösterir.

## İçerik

- `sonuclar/runs.csv` LLM'in 59 not için 3 çalışmadaki ham sonuçları (177 satır)
- `sonuclar/baseline.csv` anahtar kelime yönteminin sonuçları
- `sonuclar/calisma_bilgisi.txt` çalışma tarihi, model adı, not ve terim sayısı
- `sonuclar/metrikler.txt` `analiz.py` çıktısı (raporda kullanılan sayılar)
- `sonuclar/tablo3_ozet.txt` `tablo3.py` çıktısı
- `analiz.py` Tablo 2 metrikleri ve karmaşıklık matrisleri
- `tablo3.py` Tablo 3 için otomatik denetim
- `sekil2.py` Şekil 2 (PNG)
- Etiketli test notları ve etiketleme kuralları `src/test/resources/degerlendirme/` altındadır.

## Deneyi yeniden çalıştırma

1. Docker'ı aç ve anahtarı yalnızca bu terminalde tanımla.
2. Proje kökünde `ANTHROPIC_API_KEY=... ./mvnw test -Pdegerlendirme` komutunu çalıştır. Test gerçek API çağrısı yapar (59 not x 3 çalışma) ve yaklaşık 6 dakika sürer. Çıktılar `target/degerlendirme/` altına yazılır.
3. `runs.csv`, `baseline.csv` ve `calisma_bilgisi.txt` dosyalarını `degerlendirme/sonuclar/` klasörüne kopyala.
4. Bu klasörde `pip install -r requirements.txt` ve ardından `python analiz.py`, `python tablo3.py`, `python sekil2.py` komutlarını çalıştır.

Model yanıtları çalıştırmalar arasında değişebildiği için yeni bir çalıştırmanın metinleri ve sayıları raporda verilenlerden farklı olabilir. Raporda verilen sayılar `sonuclar/` klasöründeki 18.11.2026 tarihli çalıştırmadan gelir. Bu klasördeki ham sonuçlarla betikleri çalıştırmak aynı sayıları üretir.

## Sınırlar

- Test notlarının taslağı bir yapay zekâ asistanıyla hazırlanmıştır. Etiketler bir radyolog tarafından doğrulanmamıştır. Küme her hata türünü içerecek şekilde bilinçli kurulduğundan doğal bir not dağılımını yansıtmaz.
- Anahtar kelime listesi ve notlar çalıştırmadan önce commit edilmiştir. Sonuçlara bakıldıktan sonra etiketler veya liste değiştirilmemiştir.
- `tablo3.py` yan, ölçü, negatif bulgu ve kesinlik ifadelerinin çıktıda **bulunup bulunmadığına** bakar, anlamsal kaymayı yakalayamaz. Bu yüzden ham çıktılar ayrıca elle okunmuştur. "Yeni bilgi eklenmemesi" satırı yalnızca elle okumayla belirlenmiştir. Yeni klinik bilgi (bulgu, tanı, yan, ölçü, öneri) eklenmemiş, ancak N43 ve N54 numaralı notlarda model girdide olmayan "çapında" ifadesini eklemiştir. Bu iki not katı değerlendirmede "yeni bilgi eklendi" sayılmıştır (57/59).
- Sayısal değerler (tam eşleşme, makro F1, aciliyet doğruluğu, kaçırılan kritik not sayısı, süreler, bilgi korunma oranları) ve Tablo 3'teki elle okuma Claude (Anthropic) tarafından yapılmıştır. Koşucu proje geliştiricisinin bilgisayarında çalıştırılmıştır.
