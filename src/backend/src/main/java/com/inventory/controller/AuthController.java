package com.inventory.controller;

import com.inventory.dto.request.ForgotPasswordRequest;
import com.inventory.dto.request.GoogleAuthRequest;
import com.inventory.dto.request.LoginRequest;
import com.inventory.dto.request.ResendVerificationRequest;
import com.inventory.dto.request.ResetPasswordRequest;
import com.inventory.dto.request.SignupRequest;
import com.inventory.dto.response.AuthResponse;
import com.inventory.dto.response.UserResponse;
import com.inventory.exception.RateLimitExceededException;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.security.LoginRateLimiter;
import com.inventory.service.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final IAuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final ApiRateLimiter tokenRateLimiter;

    public AuthController(IAuthService authService, LoginRateLimiter loginRateLimiter,
                           @Qualifier("tokenRateLimiter") ApiRateLimiter tokenRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
        this.tokenRateLimiter = tokenRateLimiter;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        loginRateLimiter.checkRateLimit(httpRequest, request.email());
        try {
            AuthResponse authResponse = authService.login(request, response);
            loginRateLimiter.recordSuccessfulLogin(request.email());
            return ResponseEntity.ok(authResponse);
        } catch (BadCredentialsException e) {
            loginRateLimiter.recordFailedLogin(request.email());
            throw e;
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        loginRateLimiter.checkRateLimit(httpRequest, request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(request, response));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        loginRateLimiter.checkRateLimit(httpRequest);
        return ResponseEntity.ok(authService.googleAuth(request, response));
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam String token,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.checkRateLimit(httpRequest);
        checkTokenRateLimit(token);
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.checkRateLimit(httpRequest);
        authService.resendVerification(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.checkRateLimit(httpRequest, request.email());
        authService.forgotPassword(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        loginRateLimiter.checkRateLimit(httpRequest);
        checkTokenRateLimit(request.token());
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok().build();
    }

    private void checkTokenRateLimit(String token) {
        String prefix = token.length() > 8 ? token.substring(0, 8) : token;
        if (!tokenRateLimiter.tryAcquire("token:" + prefix).allowed()) {
            throw new RateLimitExceededException("Too many attempts. Please try again later.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(authService.getCurrentUser(userDetails.getEmail()));
    }
}
