package com.tugba.radyoloji_raporlama_asistani.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-icin-en-az-32-karakterlik-gizli-anahtar-1";
    private static final String OTHER_SECRET = "baska-bir-en-az-32-karakterlik-gizli-anahtar-2";
    private static final String EMAIL = "ayse@ornek.com";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 480);
    }

    @Test
    void generateToken_producesValidTokenCarryingEmail() {
        String token = jwtService.generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo(EMAIL);
    }

    @Test
    void generateToken_giveseachTokenItsOwnId() {
        String first = jwtService.generateToken(EMAIL);
        String second = jwtService.generateToken(EMAIL);

        assertThat(jwtService.extractTokenId(first)).isNotBlank();
        assertThat(jwtService.extractTokenId(first)).isNotEqualTo(jwtService.extractTokenId(second));
    }

    @Test
    void isTokenValid_tokenSignedWithOtherSecret_returnsFalse() {
        String foreignToken = new JwtService(OTHER_SECRET, 480).generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(foreignToken)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String expiredToken = new JwtService(SECRET, -1).generateToken(EMAIL);

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_garbageOrBlankInput_returnsFalse() {
        assertThat(jwtService.isTokenValid("bu-bir-jwt-degil")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void getRemainingSeconds_isCloseToConfiguredLifetime() {
        String token = jwtService.generateToken(EMAIL);

        assertThat(jwtService.getRemainingSeconds(token)).isBetween(28_790L, 28_800L);
    }
}
