package com.tugba.radyoloji_raporlama_asistani.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.tugba.radyoloji_raporlama_asistani.exception.TooManyRequestsException;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    private static final String USER = "ayse@ornek.com";
    private static final String KEY = "ratelimit:ai:ayse@ornek.com";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimitService = new RateLimitService(redisTemplate, 5, 60);
    }

    @Test
    void checkAiGeneration_firstRequest_setsExpiry() {
        when(valueOperations.increment(KEY)).thenReturn(1L);

        rateLimitService.checkAiGeneration(USER);

        verify(redisTemplate).expire(KEY, Duration.ofSeconds(60));
    }

    @Test
    void checkAiGeneration_withinLimit_doesNotThrowAndKeepsExpiry() {
        when(valueOperations.increment(KEY)).thenReturn(5L);

        assertThatCode(() -> rateLimitService.checkAiGeneration(USER)).doesNotThrowAnyException();

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void checkAiGeneration_overLimit_throwsTooManyRequests() {
        when(valueOperations.increment(KEY)).thenReturn(6L);

        assertThatThrownBy(() -> rateLimitService.checkAiGeneration(USER))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("60 saniye")
                .hasMessageContaining("5");
    }

    @Test
    void checkAiGeneration_usesSeparateKeyPerUser() {
        when(valueOperations.increment("ratelimit:ai:baska@ornek.com")).thenReturn(1L);

        rateLimitService.checkAiGeneration("baska@ornek.com");

        verify(valueOperations).increment("ratelimit:ai:baska@ornek.com");
        verify(valueOperations, never()).increment(KEY);
    }

    @Test
    void checkAiGeneration_redisDown_allowsRequest() {
        when(valueOperations.increment(KEY)).thenThrow(new RedisConnectionFailureException("bağlantı yok"));

        assertThatCode(() -> rateLimitService.checkAiGeneration(USER)).doesNotThrowAnyException();
    }
}
