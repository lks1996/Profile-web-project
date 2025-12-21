package com.example.profile.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Value("${app.admin.access-key}")
    private String adminKey;

    private static final String COOKIE_NAME = "ADMIN_AUTH_TOKEN";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30일 유지

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. URL 파라미터로 키가 들어왔는지 확인
        String paramKey = request.getParameter("key");

        if (adminKey.equals(paramKey)) {
            // 키가 일치하면 쿠키를 구워줌 (인증 처리)
            createAuthCookie(response);
            return true; // 통과
        }

        // 2. 파라미터가 없다면, 쿠키가 있는지 확인
        if (hasValidCookie(request)) {
            return true; // 통과
        }

        // 3. 인증 실패 시 -> 메인 페이지로 리다이렉트 (또는 403 에러)
        response.sendRedirect("/");
        return false;
    }

    private void createAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, adminKey)
                .path("/")
                .sameSite("Lax") // [중요] 모바일 호환성을 위해 Lax 설정 (Strict는 리다이렉트 시 풀릴 수 있음)
                .httpOnly(true)  // 자바스크립트 접근 차단 (보안)
                .secure(false)   // [중요] HTTPS가 아니라면 false여야 함 (SSL 적용했다면 true)
                .maxAge(COOKIE_MAX_AGE)
                .build();

        // HttpServletResponse에 헤더로 직접 추가
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private boolean hasValidCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;

        return Arrays.stream(cookies)
                .anyMatch(c -> COOKIE_NAME.equals(c.getName()) && adminKey.equals(c.getValue()));
    }
}