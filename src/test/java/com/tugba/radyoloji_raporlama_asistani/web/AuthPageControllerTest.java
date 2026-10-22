package com.tugba.radyoloji_raporlama_asistani.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tugba.radyoloji_raporlama_asistani.config.SecurityConfig;
import com.tugba.radyoloji_raporlama_asistani.dto.LoginRequest;
import com.tugba.radyoloji_raporlama_asistani.dto.RegisterRequest;
import com.tugba.radyoloji_raporlama_asistani.exception.ConflictException;
import com.tugba.radyoloji_raporlama_asistani.security.AppUserDetailsService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtCookieService;
import com.tugba.radyoloji_raporlama_asistani.security.JwtService;
import com.tugba.radyoloji_raporlama_asistani.security.TokenBlacklistService;
import com.tugba.radyoloji_raporlama_asistani.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(AuthPageController.class)
@Import(SecurityConfig.class)
class AuthPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtCookieService jwtCookieService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsService appUserDetailsService;

    // ---------- sayfaların açılması ve yönlendirmeler ----------

    @Test
    void loginPage_isOpenWithoutLogin() throws Exception {
        mockMvc.perform(get("/giris"))
                .andExpect(status().isOk())
                .andExpect(view().name("giris"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void registerPage_isOpenWithoutLoginAndOffersTitlesAndDepartments() throws Exception {
        mockMvc.perform(get("/kayit"))
                .andExpect(status().isOk())
                .andExpect(view().name("kayit"))
                .andExpect(model().attributeExists("form", "titles", "departments"));
    }

    @Test
    void protectedPage_withoutLogin_redirectsToLoginPage() throws Exception {
        mockMvc.perform(get("/raporlar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/giris"));
    }

    @Test
    void home_whenLoggedIn_redirectsToReportList() throws Exception {
        mockMvc.perform(get("/").with(user("ayse@ornek.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar"));
    }

    // ---------- giriş ----------

    @Test
    void login_blankFields_redisplaysFormWithFieldErrors() throws Exception {
        mockMvc.perform(post("/giris").param("email", "").param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("giris"))
                .andExpect(model().attributeHasFieldErrors("form", "email", "password"));

        verify(authService, never()).login(any());
    }

    @Test
    void login_validCredentials_setsCookieAndRedirectsToReportList() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn("tok123");
        when(jwtCookieService.create("tok123")).thenReturn(
                ResponseCookie.from("JWT", "tok123").httpOnly(true).path("/").build());

        MvcResult result = mockMvc.perform(post("/giris")
                .param("email", "ayse@ornek.com").param("password", "Sifre1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/raporlar"))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("JWT=tok123");
    }

    @Test
    void login_wrongCredentials_showsGenericErrorAndSetsNoCookie() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("kötü"));

        MvcResult result = mockMvc.perform(post("/giris")
                .param("email", "ayse@ornek.com").param("password", "yanlis"))
                .andExpect(status().isOk())
                .andExpect(view().name("giris"))
                .andExpect(model().attribute("errorMessage", "E-posta veya şifre hatalı."))
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    // ---------- kayıt ----------

    @Test
    void register_passwordsDoNotMatch_redisplaysFormWithError() throws Exception {
        mockMvc.perform(post("/kayit").params(registerParams("Sifre1234", "Baska1234")))
                .andExpect(status().isOk())
                .andExpect(view().name("kayit"))
                .andExpect(model().attributeHasFieldErrorCode("form", "passwordConfirm", "mismatch"));

        verify(authService, never()).register(any());
    }

    @Test
    void register_invalidFields_redisplaysFormWithFieldErrors() throws Exception {
        mockMvc.perform(post("/kayit")
                .param("fullName", "").param("email", "gecersiz")
                .param("password", "kisa").param("passwordConfirm", "kisa"))
                .andExpect(status().isOk())
                .andExpect(view().name("kayit"))
                .andExpect(model().attributeHasFieldErrors("form", "fullName", "email", "password"));

        verify(authService, never()).register(any());
    }

    @Test
    void register_duplicateEmail_showsErrorOnEmailField() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("Bu e-posta adresi zaten kayıtlı."))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/kayit").params(registerParams("Sifre1234", "Sifre1234")))
                .andExpect(status().isOk())
                .andExpect(view().name("kayit"))
                .andExpect(model().attributeHasFieldErrorCode("form", "email", "duplicate"));
    }

    @Test
    void register_validForm_redirectsToLoginWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/kayit").params(registerParams("Sifre1234", "Sifre1234")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/giris"))
                .andExpect(flash().attribute("successMessage", "Kayıt tamamlandı. Giriş yapabilirsiniz."));

        verify(authService).register(any(RegisterRequest.class));
    }

    // ---------- çıkış ----------

    @Test
    void logout_revokesTokenClearsCookieAndRedirectsToLogin() throws Exception {
        when(jwtCookieService.resolveToken(any(HttpServletRequest.class))).thenReturn("tok123");
        when(jwtCookieService.clear()).thenReturn(ResponseCookie.from("JWT", "").maxAge(Duration.ZERO).build());

        MvcResult result = mockMvc.perform(post("/cikis"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/giris"))
                .andReturn();

        verify(tokenBlacklistService).revoke("tok123");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    // ---------- yardımcı ----------

    private org.springframework.util.MultiValueMap<String, String> registerParams(String password,
            String passwordConfirm) {
        org.springframework.util.LinkedMultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
        params.add("fullName", "Ayşe Yılmaz");
        params.add("email", "ayse@ornek.com");
        params.add("password", password);
        params.add("passwordConfirm", passwordConfirm);
        params.add("phoneNumber", "0532 000 00 00");
        params.add("title", "UZM_DR");
        params.add("registrationNumber", "12345");
        params.add("department", "RADYOLOJI");
        return params;
    }
}
