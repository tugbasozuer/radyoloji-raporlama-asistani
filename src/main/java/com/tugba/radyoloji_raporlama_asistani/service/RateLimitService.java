package com.tugba.radyoloji_raporlama_asistani.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.tugba.radyoloji_raporlama_asistani.exception.TooManyRequestsException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RateLimitService {

    private static final String AI_KEY_PREFIX = "ratelimit:ai:";

    private final StringRedisTemplate redisTemplate;
    private final int aiMaxRequests;
    private final Duration aiWindow;

    public RateLimitService(StringRedisTemplate redisTemplate,
            @Value("${ratelimit.ai.max-requests}") int aiMaxRequests,
            @Value("${ratelimit.ai.window-seconds}") long aiWindowSeconds) {
        this.redisTemplate = redisTemplate;
        this.aiMaxRequests = aiMaxRequests;
        this.aiWindow = Duration.ofSeconds(aiWindowSeconds);
    }

    public void checkAiGeneration(String userEmail) {
        String key = AI_KEY_PREFIX + userEmail;
        Long count;
        try {
            count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, aiWindow);
            }
        } catch (DataAccessException e) {
            log.error("Redis'e ulaşılamıyor, YZ hız sınırı uygulanmadı", e);
            return;
        }
        if (count != null && count > aiMaxRequests) {
            throw new TooManyRequestsException("%d saniye içinde en fazla %d YZ isteği gönderebilirsiniz. "
                    .formatted(aiWindow.toSeconds(), aiMaxRequests) + "Lütfen biraz bekleyip tekrar deneyin.");
        }
    }
}
