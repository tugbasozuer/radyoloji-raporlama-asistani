package com.tugba.radyoloji_raporlama_asistani.dto;

import jakarta.validation.constraints.NotBlank;

public record ReportUpdateRequest(
        @NotBlank String findingsInput,
        String resultInput,
        boolean normalExam) {

}
