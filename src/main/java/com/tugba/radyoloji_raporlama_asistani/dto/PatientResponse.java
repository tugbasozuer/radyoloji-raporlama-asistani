package com.tugba.radyoloji_raporlama_asistani.dto;

import java.time.LocalDate;
import java.time.Period;

import com.tugba.radyoloji_raporlama_asistani.entity.Patient;

public record PatientResponse(
        Long id,
        String fullName,
        int age,
        String gender,
        String patientFileNumber,
        String maskedNationalId) {

    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFullName(),
                Period.between(patient.getBirthDate(), LocalDate.now()).getYears(),
                patient.getGender().getDisplayName(),
                patient.getPatientFileNumber(),
                mask(patient.getNationalId()));
    }

    private static String mask(String nationalId) {
        int visible = 3;
        if (nationalId.length() <= visible) {
            return nationalId;
        }
        return "*".repeat(nationalId.length() - visible)
                + nationalId.substring(nationalId.length() - visible);
    }
}
