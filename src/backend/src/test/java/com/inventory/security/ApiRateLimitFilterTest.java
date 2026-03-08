package com.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.security.ApiRateLimiter.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiRateLimitFilter Tests")
class ApiRateLimitFilterTest {

    @Mock
    private ApiRateLimiter rateLimiter;

    @Mock
    private FilterChain filterChain;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ApiRateLimitFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new ApiRateLimitFilter(rateLimiter, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/api/v1/items");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(UUID userId, String email, String role) {
        CustomUserDetails userDetails = new CustomUserDetails(userId, email, role);
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTests {

        @Test
        @DisplayName("should set rate limit headers and call filterChain when request is allowed")
        void setsHeadersAndContinuesWhenAllowed() throws ServletException, IOException {
            when(rateLimiter.tryAcquire(anyString())).thenReturn(new RateLimitResult(true, 95));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("95");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should return 429 with JSON error body and Retry-After header when request is rejected")
        void returns429WhenRejected() throws ServletException, IOException {
            when(rateLimiter.tryAcquire(anyString())).thenReturn(new RateLimitResult(false, 0));
            when(rateLimiter.getMaxRequests()).thenReturn(100);
            when(rateLimiter.getWindowSeconds()).thenReturn(60L);

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getHeader("Retry-After")).isEqualTo("60");
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

            String body = response.getContentAsString();
            assertThat(body).contains("429");
            assertThat(body).contains("Too many requests. Please try again later.");

            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("should use IP-based key for unauthenticated requests")
        void usesIpKeyForAnonymousUsers() throws ServletException, IOException {
            request.setRemoteAddr("192.168.1.100");
            when(rateLimiter.tryAcquire("ip:192.168.1.100")).thenReturn(new RateLimitResult(true, 99));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter).tryAcquire("ip:192.168.1.100");
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should use user ID key for authenticated requests")
        void usesUserIdKeyForAuthenticatedUsers() throws ServletException, IOException {
            UUID userId = UUID.randomUUID();
            setAuthentication(userId, "user@example.com", "USER");

            String expectedKey = "user:" + userId;
            when(rateLimiter.tryAcquire(expectedKey)).thenReturn(new RateLimitResult(true, 99));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter).tryAcquire(expectedKey);
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("should skip filtering for /api/v1/auth/login")
        void skipsAuthLogin() throws ServletException, IOException {
            request.setRequestURI("/api/v1/auth/login");

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter, never()).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should skip filtering for /api/v1/auth/signup")
        void skipsAuthSignup() throws ServletException, IOException {
            request.setRequestURI("/api/v1/auth/signup");

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter, never()).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should skip filtering for /actuator/health")
        void skipsActuatorHealth() throws ServletException, IOException {
            request.setRequestURI("/actuator/health");

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter, never()).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should apply filtering for /api/v1/auth/me")
        void appliesFilterForAuthMe() throws ServletException, IOException {
            request.setRequestURI("/api/v1/auth/me");
            when(rateLimiter.tryAcquire(anyString())).thenReturn(new RateLimitResult(true, 99));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should apply filtering for /api/v1/items")
        void appliesFilterForItems() throws ServletException, IOException {
            request.setRequestURI("/api/v1/items");
            when(rateLimiter.tryAcquire(anyString())).thenReturn(new RateLimitResult(true, 99));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should apply filtering for /api/v1/lists")
        void appliesFilterForLists() throws ServletException, IOException {
            request.setRequestURI("/api/v1/lists");
            when(rateLimiter.tryAcquire(anyString())).thenReturn(new RateLimitResult(true, 99));
            when(rateLimiter.getMaxRequests()).thenReturn(100);

            filter.doFilter(request, response, filterChain);

            verify(rateLimiter).tryAcquire(anyString());
            verify(filterChain).doFilter(request, response);
        }
    }
}
