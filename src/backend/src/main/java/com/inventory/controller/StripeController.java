package com.inventory.controller;

import com.inventory.enums.Role;
import com.inventory.exception.RateLimitExceededException;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IStripeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stripe")
public class StripeController {

    private static final int MAX_WEBHOOK_PAYLOAD_BYTES = 65_536;

    private final IStripeService stripeService;
    private final UserRepository userRepository;
    private final ApiRateLimiter checkoutRateLimiter;

    public StripeController(IStripeService stripeService, UserRepository userRepository,
                            @Qualifier("checkoutRateLimiter") ApiRateLimiter checkoutRateLimiter) {
        this.stripeService = stripeService;
        this.userRepository = userRepository;
        this.checkoutRateLimiter = checkoutRateLimiter;
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!checkoutRateLimiter.tryAcquire("checkout:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many checkout requests. Please try again later.");
        }
        User user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> new UserNotFoundException(userDetails.getEmail()));

        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Admin accounts cannot purchase premium"));
        }

        if (user.getRole() == Role.PREMIUM_USER) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Already a premium user"));
        }

        String checkoutUrl = stripeService.createCheckoutSession(user);
        return ResponseEntity.ok(Map.of("url", checkoutUrl));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            HttpServletRequest request,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) throws IOException {
        // Reject oversized payloads before reading the body
        if (request.getContentLengthLong() > MAX_WEBHOOK_PAYLOAD_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Payload too large");
        }
        byte[] bodyBytes = request.getInputStream().readNBytes(MAX_WEBHOOK_PAYLOAD_BYTES + 1);
        if (bodyBytes.length > MAX_WEBHOOK_PAYLOAD_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("Payload too large");
        }
        String payload = new String(bodyBytes, StandardCharsets.UTF_8);
        stripeService.handleWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getPaymentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userRepository.findById(userDetails.getId())
            .orElseThrow(() -> new UserNotFoundException(userDetails.getEmail()));

        boolean isPremium = user.getRole() == Role.PREMIUM_USER || user.getRole() == Role.ADMIN;
        return ResponseEntity.ok(Map.of("premium", isPremium));
    }
}
