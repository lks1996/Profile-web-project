package com.example.profile.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatRateLimitInterceptor implements HandlerInterceptor {

    // 1. 핵심: Redis Bean이 로컬 환경에서 존재하지 않을 수 있으므로 ObjectProvider로 감싸서 받습니다.
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Environment env;

    // 2. 로컬 환경을 위한 인메모리 저장소 (Redis 대용)
    private final Map<String, LocalRateLimit> localCache = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 5;

    // 인메모리에서 상태와 만료 시간을 관리할 내부 클래스
    private static class LocalRateLimit {
        int count = 0;
        long expiryTime = System.currentTimeMillis() + 60000; // 생성 시점으로부터 1분 뒤 만료
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null) clientIp = request.getRemoteAddr();

        String rateLimitKey = "chat:rate_limit:" + clientIp;
        boolean isLocal = isLocalProfile();

        long currentCount;

        // 환경에 따른 트래픽 제어 분기점
        if (isLocal) {
            currentCount = handleLocalRateLimit(rateLimitKey);
        } else {
            currentCount = handleRedisRateLimit(rateLimitKey);
        }

        // 컷아웃 논리 (초과 시)
        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("요청이 너무 많습니다. 1분 후에 다시 시도해주세요.");
            return false;
        }

        return true;
    }

    // 현재 활성화된 프로필이 local인지 확인하는 검증 로직
    private boolean isLocalProfile() {
        for (String profile : env.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    // 로컬 환경의 인메모리 카운트 로직 (만료 시간 시뮬레이션 포함)
    private long handleLocalRateLimit(String key) {
        long currentTime = System.currentTimeMillis();

        // ConcurrentHashMap의 원자적 연산을 사용하여 동시성 이슈 방어
        LocalRateLimit record = localCache.compute(key, (k, v) -> {
            // 데이터가 없거나, 이미 1분이 지나 만료된 경우 완전히 새로 초기화
            if (v == null || currentTime > v.expiryTime) {
                return new LocalRateLimit();
            }
            return v;
        });

        record.count++;
        return record.count;
    }

    // 운영/개발 환경의 Redis 카운트 로직
    private long handleRedisRateLimit(String key) {
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();

        // 논리적 예외 방어: 프로필은 local이 아닌데 Redis Bean이 없는 비정상 상황
        if (redisTemplate == null) {
            return 1L; // 에러를 내기보단 통과시키는 방어적 설계
        }

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count != null ? count : 1L;
    }
}