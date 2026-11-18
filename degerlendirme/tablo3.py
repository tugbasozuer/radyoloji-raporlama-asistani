"""Tablo 3: yeniden yazılan metinde bilginin korunması (yalnızca LLM) için OTOMATİK kontrol.

Her notta girdideki (olağan dışı not + sonuç notu) yan, ölçü, negatif bulgu ve kesinlik ifadesinin
modelin ham çıktısında (findings + result, sistemin eklediği standart cümle hariç) bulunup bulunmadığına bakar.
Bir not, ilgili öğeyi ÜÇ çalıştırmanın üçünde de koruduysa "korunmuş" sayılır.

Bu betik ifadelerin varlığını denetler, anlamsal kaymayı yakalayamaz. Bu yüzden ham çıktılar ayrıca elle okunmuş,
"yeni bilgi eklenmemesi" satırı yalnızca elle okumayla değerlendirilmiştir (README.md).

Çalıştırma: python tablo3.py  ->  cikti/tablo3_otomatik.csv
"""
import re

import pandas as pd

from analiz import KOK, yukle

YAN = re.compile(r"\b(sağ|sol|bilateral|her iki)\b")
OLCU = re.compile(r"(%\s?\d+|\d+(?:[.,]\d+)?\s?(?:mm|cm|%))")
OLUMSUZ = re.compile(r"izlenmemiş|saptanmamış|yoktur|\byok\b|izlenmiyor|değildir|görülmemiş")
KESINLIK = re.compile(r"şüphe|olası|uyumlu|düşünül|düşündür")


def kucuk(metin):  # Türkçe büyük/küçük harf
    return metin.replace("I", "ı").replace("İ", "i").lower()


def taraflar(metin):
    return {"bilateral" if m in ("bilateral", "her iki") else m for m in YAN.findall(kucuk(metin))}


def olculer(metin):
    return {o.replace(".", ",") for o in OLCU.findall(kucuk(metin).replace(" ", ""))}


def main():
    runs, _, notlar = yukle()
    satirlar = []
    for calisma in (1, 2, 3):
        for not_id in notlar.index:
            x = runs[(runs.not_id == not_id) & (runs.calisma == calisma)].iloc[0]
            girdi = notlar.loc[not_id, "olagan_disi_not"] + " " + notlar.loc[not_id, "sonuc_notu"]
            cikti = x.ham_findings + " " + x.ham_result
            ogeler = notlar.loc[not_id, "bilgi_ogeleri"].split("|")
            satirlar.append(dict(
                id=not_id, calisma=calisma,
                yan=taraflar(girdi) <= taraflar(cikti) if "yan" in ogeler else None,
                olcu=olculer(girdi) <= olculer(cikti) if "olcu" in ogeler else None,
                negatif=bool(OLUMSUZ.search(kucuk(cikti))) if "negatif" in ogeler else None,
                kesinlik=bool(KESINLIK.search(kucuk(cikti))) if "kesinlik" in ogeler else None,
                yeni_yan=bool(taraflar(cikti) - taraflar(girdi)),
                yeni_olcu=bool(olculer(cikti) - olculer(girdi))))
    d = pd.DataFrame(satirlar)
    for oge in ("yan", "olcu", "negatif", "kesinlik"):
        alt = d[d[oge].notna()]
        not_basina = alt.groupby("id")[oge].all()
        print("%-9s N=%2d  üç çalışmada da korunan n=%2d   (çalışma bazlı %d/%d)" % (
            oge, len(not_basina), int(not_basina.sum()), int(alt[oge].sum()), len(alt)))
    print("Çıktıda girdide olmayan yan bilgisi eklenen satır:", int(d.yeni_yan.sum()))
    print("Çıktıda girdide olmayan ölçü eklenen satır:", int(d.yeni_olcu.sum()))
    (KOK / "cikti").mkdir(exist_ok=True)
    d.to_csv(KOK / "cikti" / "tablo3_otomatik.csv", index=False)


if __name__ == "__main__":
    main()
