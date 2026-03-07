package com.inventory.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CookieService Tests")
class CookieServiceTest {

    @InjectMocks
    private CookieService cookieService;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cookieService, "jwtExpirationMs", 3600000L); // 1 hour
        ReflectionTestUtils.setField(cookieService, "secureCookie", true);
        ReflectionTestUtils.setField(cookieService, "cookieDomain", "");
    }

    @Nested
    @DisplayName("setAccessTokenCookie")
    class SetAccessTokenCookieTests {

        @Test
        @DisplayName("should set cookie with correct attributes")
        void setsCorrectAttributes() {
            cookieService.setAccessTokenCookie(response, "jwt-token-123");

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            Cookie cookie = captor.getValue();

            assertThat(cookie.getName()).isEqualTo("access_token");
            assertThat(cookie.getValue()).isEqualTo("jwt-token-123");
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.getPath()).isEqualTo("/");
            assertThat(cookie.getMaxAge()).isEqualTo(3600);
            assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
        }

        @Test
        @DisplayName("should set domain when configured")
        void setsDomainWhenConfigured() {
            ReflectionTestUtils.setField(cookieService, "cookieDomain", "example.com");

            cookieService.setAccessTokenCookie(response, "jwt-token");

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            assertThat(captor.getValue().getDomain()).isEqualTo("example.com");
        }

        @Test
        @DisplayName("should not set domain when empty")
        void noDomainWhenEmpty() {
            cookieService.setAccessTokenCookie(response, "jwt-token");

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            assertThat(captor.getValue().getDomain()).isNull();
        }

        @Test
        @DisplayName("should respect secure=false config")
        void respectsInsecureConfig() {
            ReflectionTestUtils.setField(cookieService, "secureCookie", false);

            cookieService.setAccessTokenCookie(response, "jwt-token");

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            assertThat(captor.getValue().getSecure()).isFalse();
        }
    }

    @Nested
    @DisplayName("clearAccessTokenCookie")
    class ClearAccessTokenCookieTests {

        @Test
        @DisplayName("should clear cookie with maxAge=0")
        void clearsWithMaxAgeZero() {
            cookieService.clearAccessTokenCookie(response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            Cookie cookie = captor.getValue();

            assertThat(cookie.getName()).isEqualTo("access_token");
            assertThat(cookie.getValue()).isEmpty();
            assertThat(cookie.getMaxAge()).isZero();
            assertThat(cookie.isHttpOnly()).isTrue();
            assertThat(cookie.getSecure()).isTrue();
            assertThat(cookie.getPath()).isEqualTo("/");
        }

        @Test
        @DisplayName("should set domain when configured during clear")
        void setsDomainDuringClear() {
            ReflectionTestUtils.setField(cookieService, "cookieDomain", "example.com");

            cookieService.clearAccessTokenCookie(response);

            ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
            verify(response).addCookie(captor.capture());
            assertThat(captor.getValue().getDomain()).isEqualTo("example.com");
        }
    }

    @Nested
    @DisplayName("getAccessTokenFromCookies")
    class GetAccessTokenFromCookiesTests {

        @Test
        @DisplayName("should return empty when no cookies")
        void emptyWhenNoCookies() {
            when(request.getCookies()).thenReturn(null);

            Optional<String> result = cookieService.getAccessTokenFromCookies(request);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return token when access_token cookie exists")
        void returnsTokenWhenPresent() {
            Cookie[] cookies = {
                new Cookie("other", "value"),
                new Cookie("access_token", "jwt-token-value")
            };
            when(request.getCookies()).thenReturn(cookies);

            Optional<String> result = cookieService.getAccessTokenFromCookies(request);

            assertThat(result).contains("jwt-token-value");
        }

        @Test
        @DisplayName("should return empty when access_token cookie not present")
        void emptyWhenNoAccessToken() {
            Cookie[] cookies = { new Cookie("other", "value") };
            when(request.getCookies()).thenReturn(cookies);

            Optional<String> result = cookieService.getAccessTokenFromCookies(request);

            assertThat(result).isEmpty();
        }
    }
}
