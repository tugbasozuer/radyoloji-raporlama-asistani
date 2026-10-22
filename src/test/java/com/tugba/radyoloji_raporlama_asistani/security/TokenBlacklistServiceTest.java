package com.tugba.radyoloji_raporlama_asistani.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private static final String TOKEN = "ornek.jwt.token";
    private static final String TOKEN_ID = "jti-123";
    private static final String KEY = "jwt:revoked:jti-123";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void revoke_validToken_storesTokenIdWithRemainingTtl() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(jwtService.getRemainingSeconds(TOKEN)).thenReturn(120L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.revoke(TOKEN);

        verify(valueOperations).set(KEY, "1", Duration.ofSeconds(120));
    }

    @Test
    void revoke_nullToken_doesNothing() {
        tokenBlacklistService.revoke(null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void revoke_invalidToken_doesNothing() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);

        tokenBlacklistService.revoke(TOKEN);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void revoke_tokenWithNoRemainingTime_doesNothing() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(jwtService.getRemainingSeconds(TOKEN)).thenReturn(0L);

        tokenBlacklistService.revoke(TOKEN);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void revoke_redisDown_doesNotThrow() {
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(jwtService.getRemainingSeconds(TOKEN)).thenReturn(120L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("bağlantı yok"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        assertThatCode(() -> tokenBlacklistService.revoke(TOKEN)).doesNotThrowAnyException();
    }

    @Test
    void isRevoked_keyExists_returnsTrue() {
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(redisTemplate.hasKey(KEY)).thenReturn(true);

        assertThat(tokenBlacklistService.isRevoked(TOKEN)).isTrue();
    }

    @Test
    void isRevoked_keyMissing_returnsFalse() {
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(redisTemplate.hasKey(KEY)).thenReturn(false);

        assertThat(tokenBlacklistService.isRevoked(TOKEN)).isFalse();
    }

    @Test
    void isRevoked_tokenWithoutTokenId_returnsFalse() {
        when(jwtService.extractTokenId(TOKEN)).thenReturn(null);

        assertThat(tokenBlacklistService.isRevoked(TOKEN)).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void isRevoked_redisDown_treatsTokenAsRevoked() {
        when(jwtService.extractTokenId(TOKEN)).thenReturn(TOKEN_ID);
        when(redisTemplate.hasKey(KEY)).thenThrow(new RedisConnectionFailureException("bağlantı yok"));

        assertThat(tokenBlacklistService.isRevoked(TOKEN)).isTrue();
    }
}
