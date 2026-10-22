package com.tugba.radyoloji_raporlama_asistani.web.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    @NotBlank(message = "E-posta adresini girin.")
    private String email;

    @NotBlank(message = "Şifreyi girin.")
    private String password;
}