package com.inventory.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ApiRateLimiter {

    public record RateLimitResult(boolean allowed, int remaining) {}

    private static final int MAX_ENTRIES = 50_000;
    private static final long CLEANUP_INTERVAL_MS = 60_000; // 1 minute
    private static final long MS_PER_SECOND = 1_000;

    private final int maxRequests;
    private final long windowMs;
    private final ConcurrentHashMap<String, List<Long>> requests = new ConcurrentHashMap<>();

    public ApiRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public long getWindowSeconds() {
        return windowMs / MS_PER_SECOND;
    }

    /**
     * Attempts to acquire a request slot. Returns whether the request is allowed
     * and how many requests remain in the current window.
     */
    public RateLimitResult tryAcquire(String key) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        var result = new java.util.concurrent.atomic.AtomicReference<>(new RateLimitResult(false, 0));

        requests.compute(key, (k, existing) -> {
            // LRU eviction when at capacity for a new key
            if (existing == null && requests.size() >= MAX_ENTRIES) {
                evictExpiredEntries(windowStart);
            }
            if (existing == null && requests.size() >= MAX_ENTRIES) {
                evictLruEntries(MAX_ENTRIES / 10);
            }
            if (existing == null && requests.size() >= MAX_ENTRIES) {
                log.error("Rate limiter at max capacity ({}) after eviction — circuit breaker rejecting new key: {}",
                        MAX_ENTRIES, k);
                result.set(new RateLimitResult(false, 0));
                return null;
            }

            List<Long> list = (existing != null) ? existing : new ArrayList<>();
            list.removeIf(t -> t < windowStart);
            if (list.size() < maxRequests) {
                list.add(now);
                result.set(new RateLimitResult(true, maxRequests - list.size()));
            } else {
                result.set(new RateLimitResult(false, 0));
            }
            return list.isEmpty() ? null : list;
        });

        return result.get();
    }

    @Scheduled(fixedRate = CLEANUP_INTERVAL_MS)
    public void cleanup() {
        long windowStart = System.currentTimeMillis() - windowMs;
        evictExpiredEntries(windowStart);
    }

    private void evictExpiredEntries(long windowStart) {
        requests.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(t -> t < windowStart);
            return entry.getValue().isEmpty();
        });
    }

    private void evictLruEntries(int count) {
        requests.entrySet().stream()
            .sorted(Comparator.comparingLong(e -> e.getValue().stream().mapToLong(Long::longValue).max().orElse(0L)))
            .limit(count)
            .map(java.util.Map.Entry::getKey)
            .toList()
            .forEach(requests::remove);
        log.info("LRU evicted up to {} rate limiter entries", count);
    }
}
