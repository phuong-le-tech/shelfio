package com.inventory.security;

import com.inventory.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class LoginRateLimiter {

    private final ApiRateLimiter rateLimiter;

    public LoginRateLimiter(@Qualifier("loginApiRateLimiter") ApiRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void checkRateLimit(HttpServletRequest request) {
        String ip = ClientIpResolver.resolve(request);
        ApiRateLimiter.RateLimitResult result = rateLimiter.tryAcquire(ip);
        if (!result.allowed()) {
            throw new RateLimitExceededException("Too many login attempts. Please try again later.");
        }
    }
}
