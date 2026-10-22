-- V2: rapor girdisi yeniden tasarimi.
-- Bulgular artik basliklari sabit tek bir metin olarak girilir; sablon yaklasimi birakildi.

-- Sablon tablosu artik kullanilmiyor.
DROP TABLE findings_template;

-- "Klinik bulgular / on tani" alani ekrandan kaldirildi.
ALTER TABLE report DROP COLUMN clinical_findings_and_pre_diagnosis;

-- Doktorun girdisi: Bulgular (basliklari sabit tek metin) ve Sonuc / Oneriler (serbest metin).
ALTER TABLE report RENAME COLUMN findings_exception    TO findings_input;
ALTER TABLE report RENAME COLUMN result_plan_statement TO result_input;

-- "Normal tetkik" onay kutusu.
ALTER TABLE report ADD COLUMN normal_exam BOOLEAN NOT NULL DEFAULT FALSE;
