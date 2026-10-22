package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum UrgencyLevel {
    KRITIK("Kritik"),
    ACIL("Acil"),
    RUTIN("Rutin"),
    BELIRSIZ("Belirsiz");

    private final String displayName;

    UrgencyLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
