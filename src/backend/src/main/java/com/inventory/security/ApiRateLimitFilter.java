package com.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final ApiRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String key = resolveKey(request);
        ApiRateLimiter.RateLimitResult result = rateLimiter.tryAcquire(key);

        response.setIntHeader("X-RateLimit-Limit", rateLimiter.getMaxRequests());
        response.setIntHeader("X-RateLimit-Remaining", result.remaining());

        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(rateLimiter.getWindowSeconds()));
            ApiErrorResponse error = ApiErrorResponse.of(429, "Too many requests. Please try again later.");
            objectMapper.writeValue(response.getWriter(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        // Exclude auth endpoints protected by LoginRateLimiter (but not /auth/me),
        // Stripe webhook (Stripe retries and bursts should not be rate-limited),
        // and actuator endpoints (on separate management port in prod)
        return (path.startsWith("/api/v1/auth/") && !path.equals("/api/v1/auth/me"))
            || path.equals("/api/v1/stripe/webhook")
            || path.startsWith("/actuator/");
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return "user:" + userDetails.getId();
        }
        return "ip:" + ClientIpResolver.resolve(request);
    }
}
