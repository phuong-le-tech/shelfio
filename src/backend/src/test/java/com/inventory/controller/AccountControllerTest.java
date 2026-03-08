package com.inventory.controller;

import com.inventory.config.TestSecurityConfig;
import com.inventory.exception.UserNotFoundException;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AccountController Tests")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    @Qualifier("accountDeletionRateLimiter")
    private ApiRateLimiter accountDeletionRateLimiter;

    @Nested
    @DisplayName("DELETE /api/v1/account")
    class DeleteAccountTests {

        @Test
        @DisplayName("should delete account successfully and return 204")
        void deleteAccount_authenticated_returns204() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(userId, "test@test.com", "USER");

            when(accountDeletionRateLimiter.tryAcquire("delete:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 4));
            doNothing().when(authService).deleteAccount(eq(userId), any());

            mockMvc.perform(delete("/api/v1/account")
                            .with(user(userDetails)))
                    .andExpect(status().isNoContent());

            verify(authService).deleteAccount(eq(userId), any());
        }

        @Test
        @DisplayName("should return 429 when rate limit exceeded")
        void deleteAccount_rateLimited_returns429() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(userId, "test@test.com", "USER");

            when(accountDeletionRateLimiter.tryAcquire("delete:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(false, 0));

            mockMvc.perform(delete("/api/v1/account")
                            .with(user(userDetails)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error.code").value(429))
                    .andExpect(jsonPath("$.error.message").value("Too many deletion requests. Please try again later."));

            verify(authService, never()).deleteAccount(any(), any());
        }

        @Test
        @DisplayName("should return 404 when user not found during deletion")
        void deleteAccount_userNotFound_returns404() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(userId, "test@test.com", "USER");

            when(accountDeletionRateLimiter.tryAcquire("delete:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 4));
            doThrow(new UserNotFoundException(userId)).when(authService).deleteAccount(eq(userId), any());

            mockMvc.perform(delete("/api/v1/account")
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404))
                    .andExpect(jsonPath("$.error.message").value("User not found with id: " + userId));
        }

        @Test
        @DisplayName("should work for premium user role")
        void deleteAccount_premiumUser_returns204() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(userId, "premium@test.com", "PREMIUM_USER");

            when(accountDeletionRateLimiter.tryAcquire("delete:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 4));
            doNothing().when(authService).deleteAccount(eq(userId), any());

            mockMvc.perform(delete("/api/v1/account")
                            .with(user(userDetails)))
                    .andExpect(status().isNoContent());

            verify(authService).deleteAccount(eq(userId), any());
        }

        @Test
        @DisplayName("should work for admin role")
        void deleteAccount_admin_returns204() throws Exception {
            UUID userId = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(userId, "admin@test.com", "ADMIN");

            when(accountDeletionRateLimiter.tryAcquire("delete:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 4));
            doNothing().when(authService).deleteAccount(eq(userId), any());

            mockMvc.perform(delete("/api/v1/account")
                            .with(user(userDetails)))
                    .andExpect(status().isNoContent());

            verify(authService).deleteAccount(eq(userId), any());
        }
    }
}
