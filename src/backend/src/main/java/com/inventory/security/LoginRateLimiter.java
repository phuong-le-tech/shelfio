package com.inventory.security;

import com.inventory.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private final ApiRateLimiter ipRateLimiter;
    private final ApiRateLimiter emailRateLimiter;

    public LoginRateLimiter(
            @Qualifier("loginApiRateLimiter") ApiRateLimiter ipRateLimiter,
            @Qualifier("emailLoginRateLimiter") ApiRateLimiter emailRateLimiter) {
        this.ipRateLimiter = ipRateLimiter;
        this.emailRateLimiter = emailRateLimiter;
    }

    public void checkRateLimit(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        ApiRateLimiter.RateLimitResult result = ipRateLimiter.tryAcquire(ip);
        if (!result.allowed()) {
            throw new RateLimitExceededException("Too many login attempts. Please try again later.");
        }
    }

    public void checkRateLimit(HttpServletRequest request, String email) {
        checkRateLimit(request);
        if (email != null && !email.isBlank()) {
            String emailKey = "email:" + email.toLowerCase();
            ApiRateLimiter.RateLimitResult result = emailRateLimiter.tryAcquire(emailKey);
            if (!result.allowed()) {
                throw new RateLimitExceededException("Too many attempts for this account. Please try again later.");
            }
        }
    }
}
