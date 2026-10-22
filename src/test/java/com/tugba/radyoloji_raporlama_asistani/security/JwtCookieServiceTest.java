package com.tugba.radyoloji_raporlama_asistani.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

class JwtCookieServiceTest {

    private JwtCookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new JwtCookieService(480, false);
    }

    // ---------- create / clear ----------

    @Test
    void create_buildsHttpOnlyLaxCookieWithConfiguredLifetime() {
        ResponseCookie cookie = cookieService.create("tok123");

        assertThat(cookie.getName()).isEqualTo("JWT");
        assertThat(cookie.getValue()).isEqualTo("tok123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(480));
    }

    @Test
    void create_secureFlagFollowsConfiguration() {
        assertThat(cookieService.create("tok123").isSecure()).isFalse();
        assertThat(new JwtCookieService(480, true).create("tok123").isSecure()).isTrue();
    }

    @Test
    void clear_buildsEmptyCookieThatExpiresImmediately() {
        ResponseCookie cookie = cookieService.clear();

        assertThat(cookie.getName()).isEqualTo("JWT");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    // ---------- resolveToken ----------

    @Test
    void resolveToken_bearerHeader_returnsTokenWithoutPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer tok123");

        assertThat(cookieService.resolveToken(request)).isEqualTo("tok123");
    }

    @Test
    void resolveToken_jwtCookie_returnsCookieValue() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("JWT", "cookieTok"));

        assertThat(cookieService.resolveToken(request)).isEqualTo("cookieTok");
    }

    @Test
    void resolveToken_headerAndCookie_headerWins() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer headerTok");
        request.setCookies(new Cookie("JWT", "cookieTok"));

        assertThat(cookieService.resolveToken(request)).isEqualTo("headerTok");
    }

    @Test
    void resolveToken_nonBearerAuthorizationHeader_isIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic YXlzZTpzaWZyZQ==");

        assertThat(cookieService.resolveToken(request)).isNull();
    }

    @Test
    void resolveToken_unrelatedCookie_isIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("BASKA", "deger"));

        assertThat(cookieService.resolveToken(request)).isNull();
    }

    @Test
    void resolveToken_nothingSent_returnsNull() {
        assertThat(cookieService.resolveToken(new MockHttpServletRequest())).isNull();
    }
}
