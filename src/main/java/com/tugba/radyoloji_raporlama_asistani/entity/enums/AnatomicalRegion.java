package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum AnatomicalRegion {
    KRANIAL("Kranial"),
    TORAKS("Toraks"),
    BATIN("Batın");

    private final String displayName;

    AnatomicalRegion(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
