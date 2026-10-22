package com.tugba.radyoloji_raporlama_asistani.web;

import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.tugba.radyoloji_raporlama_asistani.dto.LoginRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.RegisterRequest;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.Department;
import com.tugba.radyoloji_raporlama_asistani.entity.enums.UserTitle;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.security.JwtCookieService;
import com.tugba.radyoloji_raporlama_asistani.security.TokenBlacklistService;
import com.tugba.radyoloji_raporlama_asistani.service.AuthService;
import com.tugba.radyoloji_raporlama_asistani.web.form.LoginForm;
import com.tugba.radyoloji_raporlama_asistani.web.form.RegisterForm;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;
    private final TokenBlacklistService tokenBlacklistService;

    @ModelAttribute("titles")
    public UserTitle[] titles() {
        return UserTitle.values();
    }

    @ModelAttribute("departments")
    public Department[] departments() {
        return Department.values();
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/raporlar";
    }

    @GetMapping("/giris")
    public String loginPage(Model model) {
        model.addAttribute("form", new LoginForm());
        return "giris";
    }

    @PostMapping("/giris")
    public String login(@Valid @ModelAttribute("form") LoginForm form, BindingResult result,
            HttpServletResponse response, Model model) {
        if (result.hasErrors()) {
            return "giris";
        }
        try {
            String token = authService.login(new LoginRequest(form.getEmail(), form.getPassword()));
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookieService.create(token).toString());
            return "redirect:/raporlar";
        } catch (AuthenticationException e) {
            model.addAttribute("errorMessage", "E-posta veya şifre hatalı.");
            return "giris";
        }
    }

    @GetMapping("/kayit")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "kayit";
    }

    @PostMapping("/kayit")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (!Objects.equals(form.getPassword(), form.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "mismatch", "Şifreler aynı değil.");
        }
        if (result.hasErrors()) {
            return "kayit";
        }
        try {
            authService.register(new RegisterRequest(form.getEmail(), form.getPassword(), form.getFullName(),
                    form.getPhoneNumber(), form.getTitle(), form.getRegistrationNumber(), form.getDepartment()));
        } catch (ConflictException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
            return "kayit";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Kayıt tamamlandı. Giriş yapabilirsiniz.");
        return "redirect:/giris";
    }

    @PostMapping("/cikis")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        tokenBlacklistService.revoke(jwtCookieService.resolveToken(request));
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookieService.clear().toString());
        return "redirect:/giris";
    }
}