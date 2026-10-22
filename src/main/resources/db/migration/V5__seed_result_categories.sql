-- Sonuc kategorileri: modelin secebilecegi kapali kume.
-- approved_sentence: kategori secildiginde doktorun metninin sonuna eklenen standart oneri cumlesi.
-- 'Normal Bulgu' icin cumle, sonucun tamami olarak kullanilir (normal tetkikte doktor metin yazmaz).
INSERT INTO result_category (category_name, approved_sentence, associated_urgency_level) VALUES
    ('Normal Bulgu',           'İncelemede akut patolojik bulgu saptanmamıştır.',               'RUTIN'),
    ('Akut Kanama',            'Acil klinik değerlendirme önerilir.',                           'KRITIK'),
    ('Akut İskemi',            'Acil klinik değerlendirme önerilir.',                           'KRITIK'),
    ('Pnömotoraks',            'Acil klinik değerlendirme önerilir.',                           'KRITIK'),
    ('Pulmoner Emboli',        'Acil klinik değerlendirme önerilir.',                           'KRITIK'),
    ('Serbest Hava',           'Perforasyon açısından acil klinik değerlendirme önerilir.',     'KRITIK'),
    ('Kırık',                  'Ortopedi değerlendirmesi önerilir.',                            'ACIL'),
    ('Pnömoni',                'Klinik korelasyon önerilir.',                                   'ACIL'),
    ('Barsak Tıkanıklığı',     'Cerrahi değerlendirme önerilir.',                               'ACIL'),
    ('İleri İnceleme Önerisi', 'İleri görüntüleme ve klinik korelasyon önerilir.',              'RUTIN');