package com.tugba.radyoloji_raporlama_asistani.dto;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.AnatomicalRegion;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.ExamType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportCreateRequest(
        @NotNull Long patientId,
        @NotNull ExamType examType,
        @NotNull AnatomicalRegion anatomicalRegion,
        boolean fromEmergencyDepartment,
        @NotBlank String findingsInput,
        String resultInput,
        boolean normalExam) {

}
