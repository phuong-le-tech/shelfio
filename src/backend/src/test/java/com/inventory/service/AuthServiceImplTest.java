package com.inventory.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.inventory.dto.request.CreateUserRequest;
import com.inventory.dto.request.GoogleAuthRequest;
import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.SignupRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.enums.Role;
import com.inventory.enums.TokenType;
import com.inventory.exception.AccountNotVerifiedException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.exception.UserAlreadyExistsException;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.User;
import com.inventory.model.VerificationToken;
import com.inventory.repository.UserRepository;
import com.inventory.repository.VerificationTokenRepository;
import com.inventory.security.CookieService;
import com.inventory.security.CustomUserDetails;
import com.inventory.security.JwtService;
import com.inventory.service.impl.AuthServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CookieService cookieService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IUserService userService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Executor emailExecutor;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPassword("encoded-password");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
    }

    @Nested
    @DisplayName("signup")
    class SignupTests {

        @Test
        @DisplayName("should create user with USER role, disable account, and send verification email")
        void signup_success() {
            SignupRequest request = new SignupRequest("new@example.com", "password123", "password123");

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUser);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.signup(request, httpResponse);

            assertThat(response.user().email()).isEqualTo(testUser.getEmail());
            assertThat(response.message()).contains("verify");

            // Verify user was created with USER role
            ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
            verify(userService).createUser(captor.capture());
            assertThat(captor.getValue().role()).isEqualTo(Role.USER);
            assertThat(captor.getValue().email()).isEqualTo("new@example.com");

            // Verify no JWT cookie was set (user must verify email first)
            verify(cookieService, never()).setAccessTokenCookie(any(), anyString());

            // Verify verification email was sent
            verify(emailSender).send(eq(testUser.getEmail()), anyString(), anyString());
        }

        @Test
        @DisplayName("should throw UserAlreadyExistsException for duplicate email")
        void signup_duplicateEmail() {
            SignupRequest request = new SignupRequest("existing@example.com", "password123", "password123");

            when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(new UserAlreadyExistsException("existing@example.com"));

            assertThatThrownBy(() -> authService.signup(request, httpResponse))
                .isInstanceOf(UserAlreadyExistsException.class);

            verify(cookieService, never()).setAccessTokenCookie(any(), anyString());
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should authenticate and set JWT cookie")
        void login_success() {
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            CustomUserDetails userDetails = new CustomUserDetails(
                testUser.getId(), testUser.getEmail(), testUser.getRole().name()
            );
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(jwtService.generateToken(any(UUID.class), anyString(), anyString())).thenReturn("jwt-token");

            AuthResponse response = authService.login(request, httpResponse);

            assertThat(response.user().email()).isEqualTo("test@example.com");
            assertThat(response.message()).isEqualTo("Login successful");
            verify(cookieService).setAccessTokenCookie(httpResponse, "jwt-token");
        }

        @Test
        @DisplayName("should throw AccountNotVerifiedException for unverified account")
        void login_unverifiedAccount() {
            testUser.setEnabled(false);
            LoginRequest request = new LoginRequest("test@example.com", "password123");
            CustomUserDetails userDetails = new CustomUserDetails(
                testUser.getId(), testUser.getEmail(), testUser.getRole().name()
            );
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null);

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.login(request, httpResponse))
                .isInstanceOf(AccountNotVerifiedException.class)
                .hasMessageContaining("not verified");

            verify(cookieService, never()).setAccessTokenCookie(any(), anyString());
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUserTests {

        @Test
        @DisplayName("should return user by email")
        void getCurrentUser_success() {
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            var response = authService.getCurrentUser("test@example.com");

            assertThat(response.email()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("should throw UserNotFoundException for unknown email")
        void getCurrentUser_notFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUser("unknown@example.com"))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("logout")
    class LogoutTests {

        @Test
        @DisplayName("should clear access token cookie")
        void logout_success() {
            authService.logout(httpResponse);

            verify(cookieService).clearAccessTokenCookie(httpResponse);
        }
    }

    @Nested
    @DisplayName("googleAuth")
    class GoogleAuthTests {

        private static final ObjectMapper mapper = new ObjectMapper();

        private ObjectNode validUserInfo() {
            ObjectNode node = mapper.createObjectNode();
            node.put("sub", "google-id-123");
            node.put("email", "google@example.com");
            node.put("picture", "https://lh3.googleusercontent.com/photo.jpg");
            return node;
        }

        private void mockGoogleUserInfoResponse(JsonNode body) {
            when(restTemplate.exchange(
                eq("https://www.googleapis.com/oauth2/v3/userinfo"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
            )).thenReturn(ResponseEntity.ok(body));
        }

        @Test
        @DisplayName("should authenticate existing Google user")
        void googleAuth_existingGoogleUser() {
            mockGoogleUserInfoResponse(validUserInfo());

            User googleUser = new User();
            googleUser.setId(UUID.randomUUID());
            googleUser.setEmail("google@example.com");
            googleUser.setGoogleId("google-id-123");
            googleUser.setRole(Role.USER);
            when(userRepository.findByGoogleId("google-id-123")).thenReturn(Optional.of(googleUser));
            when(jwtService.generateToken(any(UUID.class), anyString(), anyString())).thenReturn("jwt-token");

            AuthResponse response = authService.googleAuth(new GoogleAuthRequest("valid-token"), httpResponse);

            assertThat(response.user().email()).isEqualTo("google@example.com");
            assertThat(response.message()).isEqualTo("Google authentication successful");
            verify(cookieService).setAccessTokenCookie(httpResponse, "jwt-token");
            verify(userRepository, never()).findByEmail(anyString());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should link Google account to existing email user")
        void googleAuth_linkToExistingEmailUser() {
            mockGoogleUserInfoResponse(validUserInfo());

            User existingUser = new User();
            existingUser.setId(UUID.randomUUID());
            existingUser.setEmail("google@example.com");
            existingUser.setRole(Role.USER);
            when(userRepository.findByGoogleId("google-id-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(UUID.class), anyString(), anyString())).thenReturn("jwt-token");

            AuthResponse response = authService.googleAuth(new GoogleAuthRequest("valid-token"), httpResponse);

            assertThat(response.user().email()).isEqualTo("google@example.com");
            assertThat(response.user().hasGoogleAccount()).isTrue();

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getGoogleId()).isEqualTo("google-id-123");
            assertThat(captor.getValue().getPictureUrl()).isEqualTo("https://lh3.googleusercontent.com/photo.jpg");
        }

        @Test
        @DisplayName("should create new user for unknown Google account")
        void googleAuth_createNewUser() {
            mockGoogleUserInfoResponse(validUserInfo());

            when(userRepository.findByGoogleId("google-id-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("google@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(UUID.randomUUID());
                return u;
            });
            when(jwtService.generateToken(any(UUID.class), anyString(), anyString())).thenReturn("jwt-token");

            AuthResponse response = authService.googleAuth(new GoogleAuthRequest("valid-token"), httpResponse);

            assertThat(response.user().email()).isEqualTo("google@example.com");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getGoogleId()).isEqualTo("google-id-123");
            assertThat(saved.getEmail()).isEqualTo("google@example.com");
            assertThat(saved.getRole()).isEqualTo(Role.USER);
            assertThat(saved.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid token")
        void googleAuth_invalidToken() {
            when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
            )).thenThrow(new RestClientException("401 Unauthorized"));

            assertThatThrownBy(() -> authService.googleAuth(new GoogleAuthRequest("bad-token"), httpResponse))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid Google credential");
        }

        @Test
        @DisplayName("should throw UnauthorizedException when userinfo missing required fields")
        void googleAuth_missingFields() {
            ObjectNode incomplete = mapper.createObjectNode();
            incomplete.put("sub", "google-id-123");
            // missing "email"
            mockGoogleUserInfoResponse(incomplete);

            assertThatThrownBy(() -> authService.googleAuth(new GoogleAuthRequest("valid-token"), httpResponse))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid Google user info response");
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmailTests {

        @Test
        @DisplayName("should enable user when token is valid and not expired")
        void verifyEmail_success() {
            VerificationToken token = new VerificationToken();
            token.setToken("valid-token");
            token.setUser(testUser);
            token.setType(TokenType.EMAIL_VERIFICATION);
            token.setExpiresAt(LocalDateTime.now().plusHours(1));
            testUser.setEnabled(false);

            when(verificationTokenRepository.findByTokenAndType("valid-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail("valid-token");

            verify(verificationTokenRepository).delete(token);
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should throw UnauthorizedException for expired token and delete it")
        void verifyEmail_expiredToken() {
            VerificationToken token = new VerificationToken();
            token.setToken("expired-token");
            token.setUser(testUser);
            token.setType(TokenType.EMAIL_VERIFICATION);
            token.setExpiresAt(LocalDateTime.now().minusHours(1));

            when(verificationTokenRepository.findByTokenAndType("expired-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");

            verify(verificationTokenRepository).delete(token);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid token")
        void verifyEmail_invalidToken() {
            when(verificationTokenRepository.findByTokenAndType("bad-token", TokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPasswordTests {

        @Test
        @DisplayName("should reset password when token is valid")
        void resetPassword_success() {
            VerificationToken token = new VerificationToken();
            token.setToken("reset-token");
            token.setUser(testUser);
            token.setType(TokenType.PASSWORD_RESET);
            token.setExpiresAt(LocalDateTime.now().plusMinutes(10));

            when(verificationTokenRepository.findByTokenAndType("reset-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword("reset-token", "newPassword123");

            verify(verificationTokenRepository).delete(token);
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encoded-new-password");
        }

        @Test
        @DisplayName("should throw UnauthorizedException for expired reset token and delete it")
        void resetPassword_expiredToken() {
            VerificationToken token = new VerificationToken();
            token.setToken("expired-reset");
            token.setUser(testUser);
            token.setType(TokenType.PASSWORD_RESET);
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

            when(verificationTokenRepository.findByTokenAndType("expired-reset", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.resetPassword("expired-reset", "newPassword123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");

            verify(verificationTokenRepository).delete(token);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw UnauthorizedException for invalid reset token")
        void resetPassword_invalidToken() {
            when(verificationTokenRepository.findByTokenAndType("bad-token", TokenType.PASSWORD_RESET))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPassword123"))
                .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("resendVerification")
    class ResendVerificationTests {

        @Test
        @DisplayName("should send verification email for unverified user")
        void resendVerification_unverifiedUser() {
            testUser.setEnabled(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(emailExecutor).execute(any(Runnable.class));
            when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.resendVerification("test@example.com");

            verify(emailSender).send(eq("test@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("should not send email for already verified user")
        void resendVerification_alreadyVerified() {
            testUser.setEnabled(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            authService.resendVerification("test@example.com");

            verify(emailSender, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should not send email for nonexistent user")
        void resendVerification_nonexistentUser() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            authService.resendVerification("nonexistent@example.com");

            verify(emailSender, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("forgotPassword")
    class ForgotPasswordTests {

        @Test
        @DisplayName("should send password reset email for enabled user")
        void forgotPassword_enabledUser() {
            testUser.setEnabled(true);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            doAnswer(inv -> { ((Runnable) inv.getArgument(0)).run(); return null; })
                .when(emailExecutor).execute(any(Runnable.class));
            when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.forgotPassword("test@example.com");

            verify(emailSender).send(eq("test@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("should not send email for disabled user")
        void forgotPassword_disabledUser() {
            testUser.setEnabled(false);
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            authService.forgotPassword("test@example.com");

            verify(emailSender, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("should not send email for nonexistent user")
        void forgotPassword_nonexistentUser() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            authService.forgotPassword("unknown@example.com");

            verify(emailSender, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccountTests {

        @Test
        @DisplayName("should delete user and clear cookie")
        void deleteAccount_success() {
            UUID userId = testUser.getId();

            authService.deleteAccount(userId, httpResponse);

            verify(userService).deleteUser(userId);
            verify(cookieService).clearAccessTokenCookie(httpResponse);
        }
    }

    @Nested
    @DisplayName("sanitizePictureUrl")
    class SanitizePictureUrlTests {

        private String invokeSanitize(String url) {
            return (String) org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(authService, "sanitizePictureUrl", url);
        }

        @Test
        @DisplayName("should accept valid Google profile URL")
        void acceptsGoogleUrl() {
            assertThat(invokeSanitize("https://lh3.googleusercontent.com/photo.jpg"))
                .isEqualTo("https://lh3.googleusercontent.com/photo.jpg");
        }

        @Test
        @DisplayName("should accept googleapis.com URL")
        void acceptsGoogleApisUrl() {
            assertThat(invokeSanitize("https://storage.googleapis.com/avatar.png"))
                .isEqualTo("https://storage.googleapis.com/avatar.png");
        }

        @Test
        @DisplayName("should reject non-HTTPS URL")
        void rejectsNonHttps() {
            assertThat(invokeSanitize("http://lh3.googleusercontent.com/photo.jpg")).isNull();
        }

        @Test
        @DisplayName("should reject URL with embedded credentials")
        void rejectsEmbeddedCredentials() {
            assertThat(invokeSanitize("https://user:pass@lh3.googleusercontent.com/photo.jpg")).isNull();
        }

        @Test
        @DisplayName("should reject IPv4 address host")
        void rejectsIpv4() {
            assertThat(invokeSanitize("https://192.168.1.1/photo.jpg")).isNull();
        }

        @Test
        @DisplayName("should reject non-Google domain")
        void rejectsNonGoogleDomain() {
            assertThat(invokeSanitize("https://evil.com/photo.jpg")).isNull();
        }

        @Test
        @DisplayName("should return null for null or blank URL")
        void handlesNullAndBlank() {
            assertThat(invokeSanitize(null)).isNull();
            assertThat(invokeSanitize("")).isNull();
            assertThat(invokeSanitize("   ")).isNull();
        }

        @Test
        @DisplayName("should return null for invalid URI")
        void handlesInvalidUri() {
            assertThat(invokeSanitize("not a valid uri %%%")).isNull();
        }
    }
}
