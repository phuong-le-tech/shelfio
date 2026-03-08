package com.inventory.security;

import com.inventory.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimiter Tests")
class LoginRateLimiterTest {

    @Mock
    private ApiRateLimiter ipRateLimiter;

    @Mock
    private ApiRateLimiter emailRateLimiter;

    @Mock
    private HttpServletRequest request;

    private LoginRateLimiter loginRateLimiter;

    private static final String TEST_IP = "192.168.1.1";
    private static final ApiRateLimiter.RateLimitResult ALLOWED = new ApiRateLimiter.RateLimitResult(true, 4);
    private static final ApiRateLimiter.RateLimitResult DENIED = new ApiRateLimiter.RateLimitResult(false, 0);

    @BeforeEach
    void setUp() {
        loginRateLimiter = new LoginRateLimiter(ipRateLimiter, emailRateLimiter);
        when(request.getRemoteAddr()).thenReturn(TEST_IP);
    }

    @Nested
    @DisplayName("checkRateLimit(request) - IP only")
    class CheckRateLimitByIpTests {

        @Test
        @DisplayName("should pass when IP rate limit allows")
        void passesWhenIpRateLimitAllows() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(ALLOWED);

            assertThatCode(() -> loginRateLimiter.checkRateLimit(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw RateLimitExceededException when IP limit exceeded")
        void throwsWhenIpLimitExceeded() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(DENIED);

            assertThatThrownBy(() -> loginRateLimiter.checkRateLimit(request))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessage("Too many login attempts. Please try again later.");
        }
    }

    @Nested
    @DisplayName("checkRateLimit(request, email) - IP + email")
    class CheckRateLimitByIpAndEmailTests {

        @Test
        @DisplayName("should pass when both IP and email rate limits allow")
        void passesWhenBothAllow() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(ALLOWED);
            when(emailRateLimiter.tryAcquire("email:user@example.com")).thenReturn(ALLOWED);

            assertThatCode(() -> loginRateLimiter.checkRateLimit(request, "user@example.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should throw when IP limit exceeded without checking email")
        void throwsWhenIpLimitExceededWithoutCheckingEmail() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(DENIED);

            assertThatThrownBy(() -> loginRateLimiter.checkRateLimit(request, "user@example.com"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessage("Too many login attempts. Please try again later.");

            verify(emailRateLimiter, never()).tryAcquire(anyString());
        }

        @Test
        @DisplayName("should throw when email limit exceeded after IP passes")
        void throwsWhenEmailLimitExceeded() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(ALLOWED);
            when(emailRateLimiter.tryAcquire("email:user@example.com")).thenReturn(DENIED);

            assertThatThrownBy(() -> loginRateLimiter.checkRateLimit(request, "user@example.com"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessage("Too many attempts for this account. Please try again later.");
        }

        @Test
        @DisplayName("should not check email rate limit when email is null")
        void noEmailCheckWhenNull() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(ALLOWED);

            assertThatCode(() -> loginRateLimiter.checkRateLimit(request, null))
                    .doesNotThrowAnyException();

            verify(emailRateLimiter, never()).tryAcquire(anyString());
        }

        @Test
        @DisplayName("should not check email rate limit when email is blank")
        void noEmailCheckWhenBlank() {
            when(ipRateLimiter.tryAcquire(TEST_IP)).thenReturn(ALLOWED);

            assertThatCode(() -> loginRateLimiter.checkRateLimit(request, "   "))
                    .doesNotThrowAnyException();

            verify(emailRateLimiter, never()).tryAcquire(anyString());
        }
    }
}
