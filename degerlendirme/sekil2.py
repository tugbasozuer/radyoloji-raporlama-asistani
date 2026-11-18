"""Şekil 2: aciliyet düzeyi için iki karmaşıklık matrisi (şeffaf arka planlı PNG).

Çalıştırma: python sekil2.py  ->  cikti/sekil2_karmasiklik.png
"""
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.colors import LinearSegmentedColormap
from matplotlib.patches import Rectangle

from analiz import KOK, metrikler, yukle

ETIKET = ["Kritik", "Acil", "Rutin", "Belirsiz"]
TON = ["#cde2fb", "#9ec5f4", "#6da7ec", "#3987e5", "#2a78d6", "#1c5cab", "#104281", "#0d366b"]
YAZI, YAZI_SOLUK, IZGARA = "#0b0b0b", "#52514e", "#d9d8d3"
EN_BUYUK = 26  # iki panelde ortak renk ölçeği: en koyu hücre = en çok not


def panel(ax, matris, baslik, cmap):
    deger = matris.values
    for i in range(4):
        for j in range(4):
            v = deger[i, j]
            if v > 0:
                renk = cmap(v / EN_BUYUK)
                parlaklik = 0.2126 * renk[0] + 0.7152 * renk[1] + 0.0722 * renk[2]
                yazi = "#ffffff" if parlaklik < 0.30 else YAZI
                ax.add_patch(Rectangle((j + .03, 3 - i + .03), .94, .94, fc=renk, ec="none"))
            else:
                yazi = YAZI_SOLUK
                ax.add_patch(Rectangle((j + .03, 3 - i + .03), .94, .94, fc="none", ec=IZGARA, lw=.6))
            ax.text(j + .5, 3 - i + .5, str(v), ha="center", va="center", fontsize=9.5, color=yazi,
                    fontweight="bold" if v > 0 else "normal")
    ax.set_xlim(0, 4); ax.set_ylim(0, 4); ax.set_aspect("equal")
    ax.set_xticks(np.arange(4) + .5); ax.set_xticklabels(ETIKET, fontsize=8, color=YAZI)
    ax.set_yticks(np.arange(4) + .5); ax.set_yticklabels(ETIKET[::-1], fontsize=8, color=YAZI)
    ax.tick_params(length=0, pad=3)
    for s in ax.spines.values():
        s.set_visible(False)
    ax.set_xlabel("Tahmin edilen aciliyet", fontsize=8.5, color=YAZI_SOLUK, labelpad=4)
    ax.set_title(baslik, fontsize=9, color=YAZI, pad=7)


def main():
    runs, baseline, notlar = yukle()
    b = metrikler(baseline, notlar)
    llm1 = metrikler(runs[runs.calisma == 1], notlar)
    plt.rcParams["font.family"] = "DejaVu Sans"
    cmap = LinearSegmentedColormap.from_list("mavi", TON)
    fig, eksenler = plt.subplots(1, 2, figsize=(6.6, 3.55), dpi=300)
    panel(eksenler[0], b["matris"], "(a) Anahtar kelime (temel)", cmap)
    panel(eksenler[1], llm1["matris"], "(b) LLM + doğrulama (1. çalıştırma)", cmap)
    eksenler[0].set_ylabel("Gerçek aciliyet", fontsize=8.5, color=YAZI_SOLUK, labelpad=4)
    plt.tight_layout(w_pad=1.2)
    cikti = KOK / "cikti"
    cikti.mkdir(exist_ok=True)
    fig.savefig(cikti / "sekil2_karmasiklik.png", transparent=True)
    print("Yazıldı:", cikti / "sekil2_karmasiklik.png")


if __name__ == "__main__":
    main()
