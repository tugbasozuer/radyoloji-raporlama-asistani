package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum ReportStatus {
    TASLAK("Taslak"),
    ONAYLANDI("Onaylandı");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
