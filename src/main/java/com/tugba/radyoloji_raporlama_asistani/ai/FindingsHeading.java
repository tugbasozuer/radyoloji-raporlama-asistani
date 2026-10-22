package com.tugba.radyoloji_raporlama_asistani.ai;

import java.util.List;

public enum FindingsHeading {
    OLAGAN_DISI("Olağan Dışı Bulgular", null, "olagan disi bulgular", "olagan disi", "bulgular"),
    IZLENMEMISTIR("İzlenmemiştir", "izlenmemiştir", "izlenmemistir"),
    SAPTANMAMISTIR("Saptanmamıştır", "saptanmamıştır", "saptanmamistir"),
    ACIKTIR("Açıktır", "açıktır", "aciktir"),
    DOGALDIR("Doğaldır", "doğaldır", "dogaldir");

    private final String label;
    private final String predicate;
    private final List<String> aliases;

    FindingsHeading(String label, String predicate, String... aliases) {
        this.label = label;
        this.predicate = predicate;
        this.aliases = List.of(aliases);
    }

    public String getLabel() {
        return label;
    }

    public String getPredicate() {
        return predicate;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean isPredicateHeading() {
        return predicate != null;
    }
}