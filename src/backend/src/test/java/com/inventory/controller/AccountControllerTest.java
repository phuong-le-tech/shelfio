package com.inventory.controller;

import com.inventory.config.TestSecurityConfig;
import com.inventory.enums.ItemStatus;
import com.inventory.enums.Role;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    @Qualifier("dataExportRateLimiter")
    private ApiRateLimiter dataExportRateLimiter;

    private UUID userId;
    private CustomUserDetails userDetails;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "test@test.com", "USER");

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@test.com");
        testUser.setRole(Role.USER);
        testUser.setPassword("encodedPassword");
        testUser.setCreatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        testUser.setUpdatedAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        testUser.setItemLists(new ArrayList<>());
    }

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

    @Nested
    @DisplayName("GET /api/v1/account/export")
    class ExportDataTests {

        @Test
        @DisplayName("should return user data export successfully")
        void exportData_authenticated_returnsData() throws Exception {
            when(dataExportRateLimiter.tryAcquire("export:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 0));

            ItemList list = new ItemList();
            list.setName("My List");
            list.setDescription("Test description");
            list.setCategory("Electronics");
            list.setCustomFieldDefinitions(List.of());
            list.setCreatedAt(LocalDateTime.of(2025, 1, 2, 0, 0));

            Item item = new Item();
            item.setName("Test Item");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(5);
            item.setCustomFieldValues(Map.of());
            item.setCreatedAt(LocalDateTime.of(2025, 1, 3, 0, 0));
            item.setUpdatedAt(LocalDateTime.of(2025, 1, 3, 0, 0));
            list.setItems(List.of(item));

            testUser.setItemLists(List.of(list));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            mockMvc.perform(get("/api/v1/account/export")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.profile.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.profile.role").value("USER"))
                    .andExpect(jsonPath("$.data.lists[0].name").value("My List"))
                    .andExpect(jsonPath("$.data.lists[0].items[0].name").value("Test Item"))
                    .andExpect(jsonPath("$.data.lists[0].items[0].status").value("AVAILABLE"))
                    .andExpect(jsonPath("$.data.lists[0].items[0].stock").value(5));
        }

        @Test
        @DisplayName("should return 429 when rate limit exceeded")
        void exportData_rateLimited_returns429() throws Exception {
            when(dataExportRateLimiter.tryAcquire("export:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(false, 0));

            mockMvc.perform(get("/api/v1/account/export")
                            .with(user(userDetails)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.error.code").value(429))
                    .andExpect(jsonPath("$.error.message").value("Too many export requests. Please try again later."));

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void exportData_userNotFound_returns404() throws Exception {
            when(dataExportRateLimiter.tryAcquire("export:user:" + userId))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(true, 0));
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/account/export")
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/account/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("should change password successfully")
        void changePassword_valid_returns200() throws Exception {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123", "encodedPassword")).thenReturn(true);
            when(passwordEncoder.encode("NewPassword456")).thenReturn("newEncodedPassword");

            mockMvc.perform(patch("/api/v1/account/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"OldPassword123\",\"newPassword\":\"NewPassword456\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.message").value("Password updated successfully"));

            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should return 400 when current password is incorrect")
        void changePassword_wrongCurrent_returns400() throws Exception {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("WrongPassword1", "encodedPassword")).thenReturn(false);

            mockMvc.perform(patch("/api/v1/account/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"WrongPassword1\",\"newPassword\":\"NewPassword456\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.error").value("Current password is incorrect"));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 for Google-only account")
        void changePassword_googleAccount_returns400() throws Exception {
            testUser.setPassword(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            mockMvc.perform(patch("/api/v1/account/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"OldPassword123\",\"newPassword\":\"NewPassword456\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.data.error").value("Cannot change password for Google-only accounts"));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should return 400 when new password is too short")
        void changePassword_shortPassword_returns400() throws Exception {
            mockMvc.perform(patch("/api/v1/account/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"OldPassword123\",\"newPassword\":\"Short1\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void changePassword_userNotFound_returns404() throws Exception {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            mockMvc.perform(patch("/api/v1/account/password")
                            .with(user(userDetails))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"OldPassword123\",\"newPassword\":\"NewPassword456\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }
    }
}
