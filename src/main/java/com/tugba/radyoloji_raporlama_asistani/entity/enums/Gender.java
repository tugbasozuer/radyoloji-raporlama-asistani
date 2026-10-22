package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum Gender {
    KADIN("Kadın"),
    ERKEK("Erkek");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
