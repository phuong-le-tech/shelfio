package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.TestSecurityConfig;
import com.inventory.dto.request.ForgotPasswordRequest;
import com.inventory.dto.request.GoogleAuthRequest;
import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.ResendVerificationRequest;
import com.inventory.dto.request.ResetPasswordRequest;
import com.inventory.dto.request.SignupRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.enums.Role;
import com.inventory.security.CustomUserDetails;
import com.inventory.security.LoginRateLimiter;
import com.inventory.service.IAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    private UUID userId;
    private UserResponse userResponse;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userResponse = new UserResponse(
                userId,
                "test@test.com",
                Role.USER,
                null,
                false,
                true,
                LocalDateTime.now()
        );
        authResponse = new AuthResponse(userResponse, "Success");

        doNothing().when(loginRateLimiter).checkRateLimit(any());
        doNothing().when(loginRateLimiter).checkRateLimit(any(), any());
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login with valid credentials and return auth response")
        void login_validCredentials_returnsOkWithAuthResponse() throws Exception {
            LoginRequest request = new LoginRequest("test@test.com", "password123");
            when(authService.login(any(LoginRequest.class), any())).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.message").value("Success"));

            verify(loginRateLimiter).checkRateLimit(any(), eq("test@test.com"));
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void login_blankEmail_returns400() throws Exception {
            LoginRequest request = new LoginRequest("", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void login_blankPassword_returns400() throws Exception {
            LoginRequest request = new LoginRequest("test@test.com", "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/signup")
    class SignupTests {

        @Test
        @DisplayName("should signup with valid request and return 201")
        void signup_validRequest_returns201WithAuthResponse() throws Exception {
            SignupRequest request = new SignupRequest(
                    "newuser@test.com", "StrongPass1!xx", "StrongPass1!xx");
            when(authService.signup(any(SignupRequest.class), any())).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.user.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.message").value("Success"));

            verify(loginRateLimiter).checkRateLimit(any(), eq("newuser@test.com"));
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void signup_invalidEmail_returns400() throws Exception {
            SignupRequest request = new SignupRequest(
                    "not-an-email", "StrongPass1!xx", "StrongPass1!xx");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when passwords do not match")
        void signup_passwordMismatch_returns400() throws Exception {
            SignupRequest request = new SignupRequest(
                    "newuser@test.com", "StrongPass1!xx", "DifferentPass1!xx");

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/google")
    class GoogleAuthTests {

        @Test
        @DisplayName("should authenticate with Google credential and return auth response")
        void googleAuth_validCredential_returnsOkWithAuthResponse() throws Exception {
            GoogleAuthRequest request = new GoogleAuthRequest("google-access-token-123");
            when(authService.googleAuth(any(GoogleAuthRequest.class), any())).thenReturn(authResponse);

            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.user.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.message").value("Success"));

            verify(loginRateLimiter).checkRateLimit(any());
        }

        @Test
        @DisplayName("should return 400 when credential is blank")
        void googleAuth_blankCredential_returns400() throws Exception {
            GoogleAuthRequest request = new GoogleAuthRequest("");

            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/verify")
    class VerifyEmailTests {

        @Test
        @DisplayName("should verify email with valid token and return 200")
        void verifyEmail_validToken_returnsOk() throws Exception {
            doNothing().when(authService).verifyEmail("valid-token-123");

            mockMvc.perform(get("/api/v1/auth/verify")
                            .param("token", "valid-token-123"))
                    .andExpect(status().isOk());

            verify(loginRateLimiter).checkRateLimit(any());
            verify(authService).verifyEmail("valid-token-123");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/resend-verification")
    class ResendVerificationTests {

        @Test
        @DisplayName("should resend verification email and return 200")
        void resendVerification_validEmail_returnsOk() throws Exception {
            ResendVerificationRequest request = new ResendVerificationRequest("test@test.com");
            doNothing().when(authService).resendVerification("test@test.com");

            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(loginRateLimiter).checkRateLimit(any());
            verify(authService).resendVerification("test@test.com");
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void resendVerification_blankEmail_returns400() throws Exception {
            ResendVerificationRequest request = new ResendVerificationRequest("");

            mockMvc.perform(post("/api/v1/auth/resend-verification")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should initiate forgot password and return 200")
        void forgotPassword_validEmail_returnsOk() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest("test@test.com");
            doNothing().when(authService).forgotPassword("test@test.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(loginRateLimiter).checkRateLimit(any(), eq("test@test.com"));
            verify(authService).forgotPassword("test@test.com");
        }

        @Test
        @DisplayName("should return 400 when email is invalid")
        void forgotPassword_invalidEmail_returns400() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest("not-an-email");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password with valid token and return 200")
        void resetPassword_validRequest_returnsOk() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest(
                    "reset-token-123", "NewStrongPass1!x");
            doNothing().when(authService).resetPassword("reset-token-123", "NewStrongPass1!x");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(loginRateLimiter).checkRateLimit(any());
            verify(authService).resetPassword("reset-token-123", "NewStrongPass1!x");
        }

        @Test
        @DisplayName("should return 400 when token is blank")
        void resetPassword_blankToken_returns400() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest(
                    "", "NewStrongPass1!x");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void resetPassword_shortPassword_returns400() throws Exception {
            ResetPasswordRequest request = new ResetPasswordRequest(
                    "reset-token-123", "short");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("should logout and return 204")
        void logout_returnsNoContent() throws Exception {
            doNothing().when(authService).logout(any());

            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isNoContent());

            verify(authService).logout(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return current user when authenticated")
        void getCurrentUser_authenticated_returnsOkWithUser() throws Exception {
            when(authService.getCurrentUser("test@test.com")).thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/auth/me")
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(userId.toString()))
                    .andExpect(jsonPath("$.data.email").value("test@test.com"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void getCurrentUser_notAuthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
