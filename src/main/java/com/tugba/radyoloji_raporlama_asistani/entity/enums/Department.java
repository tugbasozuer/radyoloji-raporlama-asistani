package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum Department {
    RADYOLOJI("Radyoloji"),
    ACIL_SERVIS("Acil Servis"),
    DIGER("Diğer");

    private final String displayName;

    Department(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
