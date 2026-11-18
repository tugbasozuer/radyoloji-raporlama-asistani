"""Değerlendirme deneyinin metriklerini hesaplar (Tablo 2, aciliyet karmaşıklık matrisleri).

Girdi : sonuclar/runs.csv (LLM, 3 çalışma), sonuclar/baseline.csv (anahtar kelime yöntemi),
        ../src/test/resources/degerlendirme/test_notlari.csv (etiketler)
Çıktı : ekrana yazdırılır. Dosyaya yazmak için: python analiz.py > sonuclar/metrikler.txt
"""
from pathlib import Path

import numpy as np
import pandas as pd

KOK = Path(__file__).resolve().parent
RUNS = KOK / "sonuclar" / "runs.csv"
BASELINE = KOK / "sonuclar" / "baseline.csv"
NOTLAR = KOK.parent / "src" / "test" / "resources" / "degerlendirme" / "test_notlari.csv"

KATEGORILER = ["Akut Kanama", "Akut İskemi", "Pnömotoraks", "Pulmoner Emboli", "Serbest Hava",
               "Kırık", "Pnömoni", "Barsak Tıkanıklığı", "İleri İnceleme Önerisi"]
ACILIYETLER = ["KRITIK", "ACIL", "RUTIN", "BELIRSIZ"]
GRUPLAR = ["dogrudan", "es_anlamli", "negatif", "yazim_hatasi", "coklu", "uymayan"]


def kume(metin):
    return frozenset(parca for parca in metin.split("|") if parca)


def yukle():
    runs = pd.read_csv(RUNS, sep=";", keep_default_na=False)
    baseline = pd.read_csv(BASELINE, sep=";", keep_default_na=False)
    notlar = pd.read_csv(NOTLAR, sep=";", keep_default_na=False).set_index("id")
    return runs, baseline, notlar


def metrikler(df, notlar):
    """Tek bir çalışmanın (veya anahtar kelime yönteminin) metriklerini hesaplar."""
    dogru_kat = {i: kume(r.dogru_kategoriler) for i, r in notlar.iterrows()}
    dogru_aciliyet = notlar.dogru_aciliyet.to_dict()
    tahmin_kat = {r.not_id: kume(r.secilen_kategoriler) for r in df.itertuples()}
    tahmin_aciliyet = {r.not_id: r.secilen_aciliyet for r in df.itertuples()}
    idler = list(notlar.index)

    tam_eslesme = np.mean([tahmin_kat[i] == dogru_kat[i] for i in idler])

    f1 = {}
    for kat in KATEGORILER:  # kategori başına F1: tp, fp, fn not bazında sayılır
        tp = sum(kat in tahmin_kat[i] and kat in dogru_kat[i] for i in idler)
        fp = sum(kat in tahmin_kat[i] and kat not in dogru_kat[i] for i in idler)
        fn = sum(kat not in tahmin_kat[i] and kat in dogru_kat[i] for i in idler)
        f1[kat] = 2 * tp / (2 * tp + fp + fn) if (2 * tp + fp + fn) else float("nan")
    makro_f1 = np.nanmean(list(f1.values()))

    aciliyet_dogrulugu = np.mean([tahmin_aciliyet[i] == dogru_aciliyet[i] for i in idler])
    kritikler = [i for i in idler if dogru_aciliyet[i] == "KRITIK"]
    kacirilan = [i for i in kritikler if tahmin_aciliyet[i] != "KRITIK"]

    matris = pd.DataFrame(0, index=ACILIYETLER, columns=ACILIYETLER)  # satır: gerçek, sütun: tahmin
    for i in idler:
        matris.loc[dogru_aciliyet[i], tahmin_aciliyet[i]] += 1

    return dict(tam_eslesme=tam_eslesme, makro_f1=makro_f1, aciliyet=aciliyet_dogrulugu,
                kacirilan=kacirilan, kritik_sayisi=len(kritikler), f1=f1, matris=matris,
                tahmin_kat=tahmin_kat, tahmin_aciliyet=tahmin_aciliyet,
                dogru_kat=dogru_kat, dogru_aciliyet=dogru_aciliyet)


def ort_ss(degerler):
    d = np.array(degerler, float)
    return d.mean(), d.std(ddof=1)  # örneklem standart sapması (n-1)


def main():
    runs, baseline, notlar = yukle()
    b = metrikler(baseline, notlar)
    llm = [metrikler(runs[runs.calisma == k], notlar) for k in (1, 2, 3)]
    grup = notlar.grup.to_dict()

    print("N =", len(notlar))
    print("ANAHTAR KELIME  tam_eslesme=%.4f makro_f1=%.4f aciliyet=%.4f kacirilan_KRITIK=%d/%d" % (
        b["tam_eslesme"], b["makro_f1"], b["aciliyet"], len(b["kacirilan"]), b["kritik_sayisi"]))
    print("  sure_us medyan=%.1f p95=%.1f" % (baseline.sure_us.median(), baseline.sure_us.quantile(.95)))
    for k, m in enumerate(llm, 1):
        print("LLM calisma %d   tam_eslesme=%.4f makro_f1=%.4f aciliyet=%.4f kacirilan_KRITIK=%d/%d" % (
            k, m["tam_eslesme"], m["makro_f1"], m["aciliyet"], len(m["kacirilan"]), m["kritik_sayisi"]))
    for ad, anahtar in (("tam_eslesme", "tam_eslesme"), ("makro_f1", "makro_f1"), ("aciliyet", "aciliyet")):
        ort, ss = ort_ss([m[anahtar] for m in llm])
        print("LLM %s ort=%.4f ss=%.4f" % (ad, ort, ss))
    print("LLM sure_ms (177 cagri) medyan=%.0f p95=%.0f" % (runs.sure_ms.median(), runs.sure_ms.quantile(.95)))
    print("hata_sayisi=%d ai_destekli_false=%d listede_olmayan_toplam=%d" % (
        (runs.hata != "").sum(), (runs.ai_destekli.astype(str) != "True").sum(), runs.listede_olmayan.sum()))

    print("\nKategori F1 (anahtar kelime | LLM 1,2,3):")
    for kat in KATEGORILER:
        print("  %-24s %.2f | %s" % (kat, b["f1"][kat], " ".join("%.2f" % m["f1"][kat] for m in llm)))

    print("\nGrup bazlı tam eşleşme (anahtar kelime | LLM ortalaması):")
    for g in GRUPLAR:
        idler = [i for i in notlar.index if grup[i] == g]
        be = np.mean([b["tahmin_kat"][i] == b["dogru_kat"][i] for i in idler])
        le = np.mean([np.mean([m["tahmin_kat"][i] == m["dogru_kat"][i] for i in idler]) for m in llm])
        print("  %-13s n=%2d  B=%.2f  L=%.2f" % (g, len(idler), be, le))

    print("\nAnahtar kelime karmaşıklık matrisi (satır gerçek, sütun tahmin):")
    print(b["matris"])
    print("LLM calisma 1 karmaşıklık matrisi:")
    print(llm[0]["matris"])
    ayni = sum(len({m["tahmin_kat"][i] for m in llm}) == 1 and len({m["tahmin_aciliyet"][i] for m in llm}) == 1
               for i in notlar.index)
    print("\n3 çalışmada aynı kategori ve aciliyeti veren not: %d / %d" % (ayni, len(notlar)))
    print("Anahtar kelime kaçırılan KRITIK:", b["kacirilan"])
    print("LLM kaçırılan KRITIK (çalışma 1,2,3):", [m["kacirilan"] for m in llm])
    print("LLM tam eşleşmeyen notlar:", sorted({i for m in llm for i in notlar.index
                                              if m["tahmin_kat"][i] != m["dogru_kat"][i]}))


if __name__ == "__main__":
    main()
