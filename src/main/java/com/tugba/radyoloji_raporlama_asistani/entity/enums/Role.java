package com.tugba.radyoloji_raporlama_asistani.entity.enums;

public enum Role {
    RADYOLOG("Radyolog"),
    ADMIN("Yönetici");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
