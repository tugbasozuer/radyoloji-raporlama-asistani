package com.tugba.radyoloji_raporlama_asistani.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String REGISTER_JSON = """
            {
              "email": "ayse@ornek.com",
              "password": "Sifre1234",
              "fullName": "Ayşe Yılmaz",
              "phoneNumber": "0532 000 00 00",
              "title": "UZM_DR",
              "registrationNumber": "12345",
              "department": "RADYOLOJI"
            }
            """;

    private static final String LOGIN_JSON = """
            { "email": "ayse@ornek.com", "password": "Sifre1234" }
            """;

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

    // ---------- register ----------

    @Test
    void register_validRequestWithoutLogin_returns201() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isCreated());

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("ayse@ornek.com");
        assertThat(captor.getValue().fullName()).isEqualTo("Ayşe Yılmaz");
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        doThrow(new ConflictException("Bu e-posta adresi zaten kayıtlı.")).when(authService)
                .register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Bu e-posta adresi zaten kayıtlı."));
    }

    @Test
    void register_invalidEmailShortPasswordBlankName_returns400WithFieldErrors() throws Exception {
        String invalidJson = """
                { "email": "gecersiz", "password": "kisa", "fullName": "" }
                """;

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void register_invalidPhoneNumber_returns400WithMessage() throws Exception {
        String invalidJson = REGISTER_JSON.replace("0532 000 00 00", "telefon-yok");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phoneNumber").value("Geçerli bir telefon numarası girin."));
    }

    // ---------- login ----------

    @Test
    void login_validCredentials_returnsTokenInBodyAndCookie() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn("tok123");
        when(jwtCookieService.create("tok123")).thenReturn(
                ResponseCookie.from("JWT", "tok123").httpOnly(true).path("/").build());

        MvcResult result = mockMvc
                .perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok123"))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("JWT=tok123").contains("HttpOnly");
    }

    @Test
    void login_wrongCredentials_returns401WithGenericMessage() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("kötü"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-posta veya şifre hatalı."));
    }

    @Test
    void login_blankFields_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{ \"email\": \"\", \"password\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_wrongHttpMethod_returns405() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- logout ----------

    @Test
    void logout_withToken_revokesItAndClearsCookie() throws Exception {
        when(jwtCookieService.resolveToken(any(HttpServletRequest.class))).thenReturn("tok123");
        when(jwtCookieService.clear()).thenReturn(ResponseCookie.from("JWT", "").maxAge(Duration.ZERO).build());

        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(tokenBlacklistService).revoke("tok123");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void logout_withoutToken_stillReturns204AndClearsCookie() throws Exception {
        when(jwtCookieService.clear()).thenReturn(ResponseCookie.from("JWT", "").maxAge(Duration.ZERO).build());

        MvcResult result = mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }
}
