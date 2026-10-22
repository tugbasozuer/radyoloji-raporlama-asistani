package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum ExamType {
    BT("BT"),
    MR("MR"),
    GRAFI("Grafi");

    private final String displayName;

    ExamType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
