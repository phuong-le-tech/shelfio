package com.inventory.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.inventory.dto.request.CreateUserRequest;
import com.inventory.dto.request.GoogleAuthRequest;
import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.SignupRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.enums.Role;
import com.inventory.enums.TokenType;
import com.inventory.exception.AccountNotVerifiedException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.User;
import com.inventory.model.VerificationToken;
import com.inventory.repository.UserRepository;
import com.inventory.repository.VerificationTokenRepository;
import com.inventory.security.CookieService;
import com.inventory.security.CustomUserDetails;
import com.inventory.security.JwtService;
import com.inventory.service.EmailSender;
import com.inventory.service.IAuthService;
import com.inventory.service.IUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final UserRepository userRepository;
    private final IUserService userService;
    private final RestTemplate restTemplate;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final Executor emailExecutor;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final int VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public AuthResponse login(@NonNull LoginRequest request, @NonNull HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getEmail())
            .orElseThrow(() -> new UserNotFoundException(userDetails.getEmail()));

        if (!user.isEnabled()) {
            throw new AccountNotVerifiedException("Account not verified. Please check your email.");
        }

        setAuthCookie(response, user);

        return new AuthResponse(
            UserResponse.fromEntity(user),
            "Login successful"
        );
    }

    @Override
    public AuthResponse signup(@NonNull SignupRequest request, @NonNull HttpServletResponse response) {
        CreateUserRequest createRequest = new CreateUserRequest(
            request.email(),
            request.password(),
            Role.USER
        );

        User user = userService.createUser(createRequest);
        user.setEnabled(false);
        user = userRepository.save(user);

        sendVerificationEmail(user);

        return new AuthResponse(
            UserResponse.fromEntity(user),
            "Signup successful. Please check your email to verify your account."
        );
    }

    @Override
    public AuthResponse googleAuth(@NonNull GoogleAuthRequest request, @NonNull HttpServletResponse response) {
        JsonNode userInfo = fetchGoogleUserInfo(request.credential());

        String googleId = userInfo.get("sub").asText();
        String email = userInfo.get("email").asText();
        String pictureUrl = userInfo.has("picture") ? sanitizePictureUrl(userInfo.get("picture").asText()) : null;

        User user = userRepository.findByGoogleId(googleId)
            .orElseGet(() -> findOrCreateGoogleUser(googleId, email, pictureUrl));

        setAuthCookie(response, user);

        return new AuthResponse(
            UserResponse.fromEntity(user),
            "Google authentication successful"
        );
    }

    @Override
    public void verifyEmail(@NonNull String token) {
        VerificationToken verificationToken = verificationTokenRepository
            .findByTokenAndType(token, TokenType.EMAIL_VERIFICATION)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired verification token"));

        boolean isExpired = verificationToken.getExpiresAt().isBefore(LocalDateTime.now());

        // Always delete to prevent replay attacks
        verificationTokenRepository.delete(verificationToken);

        if (isExpired) {
            throw new UnauthorizedException("Invalid or expired verification token");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void resendVerification(@NonNull String email) {
        long start = System.nanoTime();
        userRepository.findByEmail(email)
            .filter(user -> !user.isEnabled())
            .ifPresent(user -> CompletableFuture.runAsync(() -> sendVerificationEmail(user), emailExecutor));
        // Constant-time response to prevent timing-based email enumeration
        enforceMinimumLatency(start, 200);
    }

    @Override
    public void forgotPassword(@NonNull String email) {
        long start = System.nanoTime();
        userRepository.findByEmail(email)
            .filter(User::isEnabled)
            .ifPresent(user -> CompletableFuture.runAsync(() -> sendPasswordResetEmail(user), emailExecutor));
        // Constant-time response to prevent timing-based email enumeration
        enforceMinimumLatency(start, 200);
    }

    @Override
    public void resetPassword(@NonNull String token, @NonNull String newPassword) {
        VerificationToken resetToken = verificationTokenRepository
            .findByTokenAndType(token, TokenType.PASSWORD_RESET)
            .orElseThrow(() -> new UnauthorizedException("Invalid or expired reset token"));

        boolean isExpired = resetToken.getExpiresAt().isBefore(LocalDateTime.now());

        // Always delete to prevent replay attacks
        verificationTokenRepository.delete(resetToken);

        if (isExpired) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void logout(@NonNull HttpServletResponse response) {
        cookieService.clearAccessTokenCookie(response);
    }

    @Override
    public void deleteAccount(@NonNull UUID userId, @NonNull HttpServletResponse response) {
        userService.deleteUser(userId);
        cookieService.clearAccessTokenCookie(response);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(@NonNull String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        return UserResponse.fromEntity(user);
    }

    private void sendVerificationEmail(User user) {
        verificationTokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFICATION);

        VerificationToken token = new VerificationToken();
        token.setToken(generateSecureToken());
        token.setUser(user);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setExpiresAt(LocalDateTime.now().plusHours(VERIFICATION_TOKEN_EXPIRY_HOURS));
        verificationTokenRepository.save(token);

        String verifyUrl = frontendUrl + "/verify-email?token=" + token.getToken();
        String html = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head><body>
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Verifiez votre adresse email</h2>
                <p>Merci de vous etre inscrit sur Inventory. Cliquez sur le lien ci-dessous pour verifier votre compte :</p>
                <p><a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #171717; color: #ffffff; text-decoration: none; border-radius: 8px;">Verifier mon email</a></p>
                <p style="color: #666; font-size: 14px;">Ce lien expire dans %d heures.</p>
                <p style="color: #666; font-size: 14px;">Si vous n'avez pas cree de compte, vous pouvez ignorer cet email.</p>
            </div>
            </body></html>
            """.formatted(verifyUrl, VERIFICATION_TOKEN_EXPIRY_HOURS);

        emailSender.send(user.getEmail(), "Verifiez votre adresse email - Inventory", html);
    }

    private void sendPasswordResetEmail(User user) {
        verificationTokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);

        VerificationToken token = new VerificationToken();
        token.setToken(generateSecureToken());
        token.setUser(user);
        token.setType(TokenType.PASSWORD_RESET);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_EXPIRY_MINUTES));
        verificationTokenRepository.save(token);

        String resetUrl = frontendUrl + "/reset-password?token=" + token.getToken();
        String html = """
            <!DOCTYPE html>
            <html><head><meta charset="UTF-8"></head><body>
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto;">
                <h2>Reinitialiser votre mot de passe</h2>
                <p>Vous avez demande la reinitialisation de votre mot de passe. Cliquez sur le lien ci-dessous :</p>
                <p><a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #171717; color: #ffffff; text-decoration: none; border-radius: 8px;">Reinitialiser mon mot de passe</a></p>
                <p style="color: #666; font-size: 14px;">Ce lien expire dans %d minutes.</p>
                <p style="color: #666; font-size: 14px;">Si vous n'avez pas fait cette demande, vous pouvez ignorer cet email.</p>
            </div>
            </body></html>
            """.formatted(resetUrl, PASSWORD_RESET_TOKEN_EXPIRY_MINUTES);

        emailSender.send(user.getEmail(), "Reinitialisation du mot de passe - Inventory", html);
    }

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private void setAuthCookie(HttpServletResponse response, User user) {
        String token = jwtService.generateToken(
            user.getId(),
            user.getEmail(),
            user.getRole().name()
        );
        cookieService.setAccessTokenCookie(response, token);
    }

    private JsonNode fetchGoogleUserInfo(@NonNull String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
            );

            JsonNode body = response.getBody();
            if (body == null || !body.has("sub") || !body.has("email")) {
                throw new UnauthorizedException("Invalid Google user info response");
            }

            return body;
        } catch (RestClientException e) {
            log.warn("Failed to verify Google access token: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google credential");
        }
    }

    private String sanitizePictureUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI uri = java.net.URI.create(url);
            // Reject non-HTTPS schemes
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                log.warn("Rejected non-HTTPS picture URL scheme: {}", uri.getScheme());
                return null;
            }
            // Reject URLs with embedded credentials (user@host)
            if (uri.getUserInfo() != null) {
                log.warn("Rejected picture URL with embedded credentials");
                return null;
            }
            String host = uri.getHost();
            if (host == null) {
                log.warn("Rejected picture URL with no host");
                return null;
            }
            // Reject IP addresses (IPv4 and IPv6)
            if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}") || host.contains(":")) {
                log.warn("Rejected picture URL with IP address host");
                return null;
            }
            if (host.endsWith(".googleusercontent.com") || host.endsWith(".googleapis.com")) {
                return url;
            }
        } catch (IllegalArgumentException e) {
            // Invalid URI
        }
        log.warn("Rejected non-Google picture URL: {}", url.substring(0, Math.min(url.length(), 50)));
        return null;
    }

    private void enforceMinimumLatency(long startNanos, long minimumMs) {
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        long jitter = SECURE_RANDOM.nextInt(50);
        long targetMs = minimumMs + jitter;
        if (elapsedMs < targetMs) {
            try {
                Thread.sleep(targetMs - elapsedMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private User findOrCreateGoogleUser(String googleId, String email, String pictureUrl) {
        return userRepository.findByEmail(email)
            .map(existingUser -> {
                existingUser.setGoogleId(googleId);
                existingUser.setPictureUrl(pictureUrl);
                existingUser.setEnabled(true);
                return userRepository.save(existingUser);
            })
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setGoogleId(googleId);
                newUser.setPictureUrl(pictureUrl);
                newUser.setRole(Role.USER);
                newUser.setEnabled(true);
                return userRepository.save(newUser);
            });
    }
}
