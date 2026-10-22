package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum UserTitle {
    DR("Dr."),
    ARS_GOR_DR("Arş. Gör. Dr."),
    UZM_DR("Uzm. Dr."),
    OGR_UYESI_DR("Dr. Öğr. Üyesi"),
    DOC_DR("Doç. Dr."),
    PROF_DR("Prof. Dr.");

    private final String displayName;

    UserTitle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
