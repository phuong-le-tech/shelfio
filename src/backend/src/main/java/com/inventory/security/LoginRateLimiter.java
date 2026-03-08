package com.inventory.security;

import com.inventory.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LoginRateLimiter {

    private final ApiRateLimiter ipRateLimiter;
    private final ApiRateLimiter emailRateLimiter;
    private final ConcurrentHashMap<String, LockoutState> lockouts = new ConcurrentHashMap<>();

    private static final int LOCKOUT_TIER_1_THRESHOLD = 5;
    private static final int LOCKOUT_TIER_2_THRESHOLD = 10;
    private static final int LOCKOUT_TIER_3_THRESHOLD = 15;
    private static final long LOCKOUT_TIER_1_SECONDS = 5 * 60;       // 5 minutes
    private static final long LOCKOUT_TIER_2_SECONDS = 30 * 60;      // 30 minutes
    private static final long LOCKOUT_TIER_3_SECONDS = 24 * 60 * 60; // 24 hours
    private static final int MAX_LOCKOUT_ENTRIES = 10_000;

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
            checkLockout(email);
            String emailKey = "email:" + email.toLowerCase();
            ApiRateLimiter.RateLimitResult result = emailRateLimiter.tryAcquire(emailKey);
            if (!result.allowed()) {
                throw new RateLimitExceededException("Too many attempts for this account. Please try again later.");
            }
        }
    }

    public void recordFailedLogin(String email) {
        if (email == null || email.isBlank()) return;
        String key = email.toLowerCase();

        if (lockouts.size() >= MAX_LOCKOUT_ENTRIES && !lockouts.containsKey(key)) {
            return;
        }

        lockouts.compute(key, (k, state) -> {
            if (state == null) state = new LockoutState();
            state.failedAttempts++;
            state.lastFailedAt = Instant.now();

            if (state.failedAttempts >= LOCKOUT_TIER_3_THRESHOLD) {
                state.lockedUntil = Instant.now().plusSeconds(LOCKOUT_TIER_3_SECONDS);
            } else if (state.failedAttempts >= LOCKOUT_TIER_2_THRESHOLD) {
                state.lockedUntil = Instant.now().plusSeconds(LOCKOUT_TIER_2_SECONDS);
            } else if (state.failedAttempts >= LOCKOUT_TIER_1_THRESHOLD) {
                state.lockedUntil = Instant.now().plusSeconds(LOCKOUT_TIER_1_SECONDS);
            }

            if (state.lockedUntil != null) {
                log.warn("Account locked: email={}, failedAttempts={}, lockedUntil={}",
                        k.substring(0, Math.min(k.length(), 3)) + "***", state.failedAttempts, state.lockedUntil);
            }

            return state;
        });
    }

    public void recordSuccessfulLogin(String email) {
        if (email == null || email.isBlank()) return;
        lockouts.remove(email.toLowerCase());
    }

    private void checkLockout(String email) {
        if (email == null || email.isBlank()) return;
        LockoutState state = lockouts.get(email.toLowerCase());
        if (state != null && state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
            long minutesRemaining = Duration.between(Instant.now(), state.lockedUntil).toMinutes();
            throw new RateLimitExceededException(
                    "Account temporarily locked due to too many failed attempts. Try again in " + Math.max(1, minutesRemaining) + " minutes.");
        }
    }

    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void cleanupExpiredLockouts() {
        Instant now = Instant.now();
        lockouts.entrySet().removeIf(entry -> {
            LockoutState state = entry.getValue();
            if (state.lockedUntil != null && now.isAfter(state.lockedUntil)
                    && state.lastFailedAt != null && now.isAfter(state.lastFailedAt.plusSeconds(LOCKOUT_TIER_3_SECONDS))) {
                return true;
            }
            if (state.lockedUntil == null && state.lastFailedAt != null && now.isAfter(state.lastFailedAt.plusSeconds(3600))) {
                return true;
            }
            return false;
        });
    }

    private static class LockoutState {
        int failedAttempts;
        Instant lockedUntil;
        Instant lastFailedAt;
    }
}
