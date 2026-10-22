package com.tugba.radyoloji_raporlama_asistani.service;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tugba.radyoloji_raporlama_asistani.dto.LoginRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.RegisterRequest;
import com.tugba.radyoloji_raporlama_asistani.entity.AppUser;
import com.tugba.radyoloji_raporlama_asistani.entity.Identity;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.Role;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.repository.AppUserRepository;
import com.tugba.radyoloji_raporlama_asistani.repository.IdentityRepository;
import com.tugba.radyoloji_raporlama_asistani.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final IdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (appUserRepository.existsByEmail(email)) {
            throw new ConflictException("Bu e-posta adresi zaten kayıtlı.");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(email);
        appUser.setPasswordHash(passwordEncoder.encode(request.password()));
        appUser.setRole(Role.RADYOLOG);
        appUserRepository.save(appUser);

        Identity identity = new Identity();
        identity.setAppUser(appUser);
        identity.setFullName(request.fullName().trim());
        identity.setPhoneNumber(blankToNull(request.phoneNumber()));
        identity.setTitle(request.title());
        identity.setRegistrationNumber(blankToNull(request.registrationNumber()));
        identity.setDepartment(request.department());
        identity.setCreatedAt(LocalDateTime.now());
        identityRepository.save(identity);
    }

    public String login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        return jwtService.generateToken(email);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
