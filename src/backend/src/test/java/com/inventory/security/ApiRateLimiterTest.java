package com.inventory.security;

import com.inventory.security.ApiRateLimiter.RateLimitResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiRateLimiter Tests")
class ApiRateLimiterTest {

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_MS = 1000L;

    private ApiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new ApiRateLimiter(MAX_REQUESTS, WINDOW_MS);
    }

    @Nested
    @DisplayName("getMaxRequests")
    class GetMaxRequestsTests {

        @Test
        @DisplayName("should return configured maxRequests")
        void returnsConfiguredMaxRequests() {
            assertThat(rateLimiter.getMaxRequests()).isEqualTo(MAX_REQUESTS);
        }
    }

    @Nested
    @DisplayName("getWindowSeconds")
    class GetWindowSecondsTests {

        @Test
        @DisplayName("should return windowMs converted to seconds")
        void returnsWindowInSeconds() {
            assertThat(rateLimiter.getWindowSeconds()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should correctly convert larger window values")
        void convertsLargerValues() {
            ApiRateLimiter limiter = new ApiRateLimiter(100, 60_000L);

            assertThat(limiter.getWindowSeconds()).isEqualTo(60L);
        }
    }

    @Nested
    @DisplayName("tryAcquire")
    class TryAcquireTests {

        @Test
        @DisplayName("should allow first request")
        void allowsFirstRequest() {
            RateLimitResult result = rateLimiter.tryAcquire("user-1");

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("should allow requests up to the limit")
        void allowsRequestsUpToLimit() {
            RateLimitResult first = rateLimiter.tryAcquire("user-1");
            RateLimitResult second = rateLimiter.tryAcquire("user-1");
            RateLimitResult third = rateLimiter.tryAcquire("user-1");

            assertThat(first.allowed()).isTrue();
            assertThat(first.remaining()).isEqualTo(2);

            assertThat(second.allowed()).isTrue();
            assertThat(second.remaining()).isEqualTo(1);

            assertThat(third.allowed()).isTrue();
            assertThat(third.remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("should reject request when limit is reached")
        void rejectsWhenLimitReached() {
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");

            RateLimitResult result = rateLimiter.tryAcquire("user-1");

            assertThat(result.allowed()).isFalse();
            assertThat(result.remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("should continue rejecting after limit is exceeded")
        void continuesRejectingAfterLimit() {
            for (int i = 0; i < MAX_REQUESTS; i++) {
                rateLimiter.tryAcquire("user-1");
            }

            RateLimitResult fourth = rateLimiter.tryAcquire("user-1");
            RateLimitResult fifth = rateLimiter.tryAcquire("user-1");

            assertThat(fourth.allowed()).isFalse();
            assertThat(fifth.allowed()).isFalse();
        }

        @Test
        @DisplayName("should allow requests again after window expires")
        void allowsAfterWindowExpires() throws InterruptedException {
            for (int i = 0; i < MAX_REQUESTS; i++) {
                rateLimiter.tryAcquire("user-1");
            }

            RateLimitResult rejected = rateLimiter.tryAcquire("user-1");
            assertThat(rejected.allowed()).isFalse();

            Thread.sleep(WINDOW_MS + 50);

            RateLimitResult allowed = rateLimiter.tryAcquire("user-1");
            assertThat(allowed.allowed()).isTrue();
            assertThat(allowed.remaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return correct remaining count as requests accumulate")
        void correctRemainingCount() {
            assertThat(rateLimiter.tryAcquire("user-1").remaining()).isEqualTo(2);
            assertThat(rateLimiter.tryAcquire("user-1").remaining()).isEqualTo(1);
            assertThat(rateLimiter.tryAcquire("user-1").remaining()).isEqualTo(0);
            assertThat(rateLimiter.tryAcquire("user-1").remaining()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("multiple keys")
    class MultipleKeysTests {

        @Test
        @DisplayName("should track keys independently")
        void tracksKeysIndependently() {
            for (int i = 0; i < MAX_REQUESTS; i++) {
                rateLimiter.tryAcquire("user-1");
            }

            RateLimitResult user1Result = rateLimiter.tryAcquire("user-1");
            RateLimitResult user2Result = rateLimiter.tryAcquire("user-2");

            assertThat(user1Result.allowed()).isFalse();
            assertThat(user2Result.allowed()).isTrue();
            assertThat(user2Result.remaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("should allow each key up to the limit independently")
        void eachKeyHasOwnLimit() {
            for (int i = 0; i < MAX_REQUESTS; i++) {
                rateLimiter.tryAcquire("user-1");
                rateLimiter.tryAcquire("user-2");
            }

            assertThat(rateLimiter.tryAcquire("user-1").allowed()).isFalse();
            assertThat(rateLimiter.tryAcquire("user-2").allowed()).isFalse();
            assertThat(rateLimiter.tryAcquire("user-3").allowed()).isTrue();
        }

        @Test
        @DisplayName("should not affect other keys when one key's window expires")
        void windowExpiryIsPerKey() throws InterruptedException {
            rateLimiter.tryAcquire("user-1");

            Thread.sleep(WINDOW_MS / 2);

            for (int i = 0; i < MAX_REQUESTS; i++) {
                rateLimiter.tryAcquire("user-2");
            }

            Thread.sleep(WINDOW_MS / 2 + 50);

            // user-1's request has expired, should allow again (full capacity)
            assertThat(rateLimiter.tryAcquire("user-1").allowed()).isTrue();
            assertThat(rateLimiter.tryAcquire("user-1").remaining()).isEqualTo(1);

            // user-2's requests are still within window, should be rejected
            assertThat(rateLimiter.tryAcquire("user-2").allowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("window expiry within tryAcquire")
    class WindowExpiryTests {

        @Test
        @DisplayName("should evict expired requests and allow new ones within tryAcquire")
        void evictsExpiredRequestsOnAcquire() throws InterruptedException {
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");

            Thread.sleep(WINDOW_MS + 50);

            // Old requests should be cleaned up within tryAcquire
            RateLimitResult result = rateLimiter.tryAcquire("user-1");

            assertThat(result.allowed()).isTrue();
            assertThat(result.remaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("should partially evict expired requests and account for remaining ones")
        void partiallyEvictsExpiredRequests() throws InterruptedException {
            // Make 2 requests now
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");

            // Wait for half the window so first 2 requests age
            Thread.sleep(WINDOW_MS / 2 + 50);

            // Make a third request (still within window for this one)
            rateLimiter.tryAcquire("user-1");

            // Wait for the first 2 requests to expire but not the third
            Thread.sleep(WINDOW_MS / 2 + 50);

            // Now: first 2 expired, third still valid -> capacity should be 2 remaining
            RateLimitResult result = rateLimiter.tryAcquire("user-1");

            assertThat(result.allowed()).isTrue();
            // 1 still active from previous + 1 new = 2 used, so remaining = 1
            assertThat(result.remaining()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("cleanup")
    class CleanupTests {

        @Test
        @DisplayName("should remove expired entries on cleanup")
        void removesExpiredEntries() throws InterruptedException {
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-2");

            Thread.sleep(WINDOW_MS + 50);

            rateLimiter.cleanup();

            // After cleanup, both keys should be evicted since their requests expired
            // New requests should be allowed with full capacity
            RateLimitResult result1 = rateLimiter.tryAcquire("user-1");
            RateLimitResult result2 = rateLimiter.tryAcquire("user-2");

            assertThat(result1.allowed()).isTrue();
            assertThat(result1.remaining()).isEqualTo(2);

            assertThat(result2.allowed()).isTrue();
            assertThat(result2.remaining()).isEqualTo(2);
        }

        @Test
        @DisplayName("should not remove entries that are still within the window")
        void keepsActiveEntries() {
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");
            rateLimiter.tryAcquire("user-1");

            rateLimiter.cleanup();

            // Entries still within window, should still be rate limited
            RateLimitResult result = rateLimiter.tryAcquire("user-1");

            assertThat(result.allowed()).isFalse();
            assertThat(result.remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle cleanup when no entries exist")
        void handlesEmptyState() {
            // Should not throw
            rateLimiter.cleanup();

            RateLimitResult result = rateLimiter.tryAcquire("user-1");
            assertThat(result.allowed()).isTrue();
        }

        @Test
        @DisplayName("should selectively remove only expired entries among mixed keys")
        void selectivelyRemovesExpiredEntries() throws InterruptedException {
            rateLimiter.tryAcquire("user-expired");

            Thread.sleep(WINDOW_MS + 50);

            rateLimiter.tryAcquire("user-active");
            rateLimiter.tryAcquire("user-active");
            rateLimiter.tryAcquire("user-active");

            rateLimiter.cleanup();

            // Expired key should have full capacity
            RateLimitResult expiredResult = rateLimiter.tryAcquire("user-expired");
            assertThat(expiredResult.allowed()).isTrue();
            assertThat(expiredResult.remaining()).isEqualTo(2);

            // Active key should still be rate limited
            assertThat(rateLimiter.tryAcquire("user-active").allowed()).isFalse();
        }
    }

    @Nested
    @DisplayName("RateLimitResult record")
    class RateLimitResultTests {

        @Test
        @DisplayName("should correctly store allowed and remaining values")
        void storesValues() {
            RateLimitResult allowed = new RateLimitResult(true, 5);
            RateLimitResult denied = new RateLimitResult(false, 0);

            assertThat(allowed.allowed()).isTrue();
            assertThat(allowed.remaining()).isEqualTo(5);

            assertThat(denied.allowed()).isFalse();
            assertThat(denied.remaining()).isEqualTo(0);
        }

        @Test
        @DisplayName("should support equality for records")
        void supportsEquality() {
            RateLimitResult a = new RateLimitResult(true, 3);
            RateLimitResult b = new RateLimitResult(true, 3);

            assertThat(a).isEqualTo(b);
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle single request limit")
        void singleRequestLimit() {
            ApiRateLimiter singleLimiter = new ApiRateLimiter(1, WINDOW_MS);

            RateLimitResult first = singleLimiter.tryAcquire("user-1");
            RateLimitResult second = singleLimiter.tryAcquire("user-1");

            assertThat(first.allowed()).isTrue();
            assertThat(first.remaining()).isEqualTo(0);
            assertThat(second.allowed()).isFalse();
        }

        @Test
        @DisplayName("should handle same key with rapid successive calls")
        void rapidSuccessiveCalls() {
            for (int i = 0; i < MAX_REQUESTS; i++) {
                assertThat(rateLimiter.tryAcquire("rapid-key").allowed()).isTrue();
            }
            assertThat(rateLimiter.tryAcquire("rapid-key").allowed()).isFalse();
        }

        @Test
        @DisplayName("should handle many different keys")
        void manyDifferentKeys() {
            for (int i = 0; i < 100; i++) {
                RateLimitResult result = rateLimiter.tryAcquire("key-" + i);
                assertThat(result.allowed()).isTrue();
                assertThat(result.remaining()).isEqualTo(2);
            }
        }
    }
}
