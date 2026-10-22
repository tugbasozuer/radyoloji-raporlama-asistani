package com.tugba.radyoloji_raporlama_asistani.dto;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.Department;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UserTitle;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 255) String fullName,
        @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,30}$", message = "Geçerli bir telefon numarası girin.") String phoneNumber,
        UserTitle title,
        @Size(max = 50) String registrationNumber,
        Department department) {
}
