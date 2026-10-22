package com.tugba.radyoloji_raporlama_asistani.web.form;

import com.tugba.radyoloji_raporlama_asistani.entity.enums.Department;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UserTitle;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Ad soyad girin.")
    @Size(max = 255)
    private String fullName;

    private UserTitle title;

    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{7,30}$", message = "Geçerli bir telefon numarası girin.")
    private String phoneNumber;

    @Size(max = 50)
    private String registrationNumber;

    private Department department;

    @NotBlank(message = "E-posta adresini girin.")
    @Email(message = "Geçerli bir e-posta adresi girin.")
    private String email;

    @NotBlank(message = "Şifre girin.")
    @Size(min = 8, max = 72, message = "Şifre 8 ile 72 karakter arasında olmalı.")
    private String password;

    @NotBlank(message = "Şifreyi tekrar girin.")
    private String passwordConfirm;
}