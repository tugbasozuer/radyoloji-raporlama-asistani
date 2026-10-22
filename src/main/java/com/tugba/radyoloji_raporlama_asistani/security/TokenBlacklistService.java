package com.tugba.radyoloji_raporlama_asistani.security;

import java.time.Duration;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:revoked:";

    private final StringRedisTemplate redisTemplate;
    private final JwtService jwtService;

    public void revoke(String token) {
        if (token == null || !jwtService.isTokenValid(token)) {
            return;
        }
        String tokenId = jwtService.extractTokenId(token);
        long remainingSeconds = jwtService.getRemainingSeconds(token);
        if (tokenId == null || remainingSeconds <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, "1", Duration.ofSeconds(remainingSeconds));
        } catch (DataAccessException e) {
            log.error("Token iptal edilemedi, Redis'e ulaşılamıyor", e);
        }
    }

    public boolean isRevoked(String token) {
        String tokenId = jwtService.extractTokenId(token);
        if (tokenId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + tokenId));
        } catch (DataAccessException e) {
            log.error("Redis'e ulaşılamıyor, token iptal edilmiş sayıldı", e);
            return true;
        }
    }
}
