package com.inventory.controller;

import com.inventory.dto.request.ChangePasswordRequest;
import com.inventory.exception.RateLimitExceededException;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.UserRepository;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final IAuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiRateLimiter accountDeletionRateLimiter;
    private final ApiRateLimiter dataExportRateLimiter;

    public AccountController(IAuthService authService,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             @Qualifier("accountDeletionRateLimiter") ApiRateLimiter accountDeletionRateLimiter,
                             @Qualifier("dataExportRateLimiter") ApiRateLimiter dataExportRateLimiter) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountDeletionRateLimiter = accountDeletionRateLimiter;
        this.dataExportRateLimiter = dataExportRateLimiter;
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletResponse response
    ) {
        if (!accountDeletionRateLimiter.tryAcquire("delete:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many deletion requests. Please try again later.");
        }
        log.info("SECURITY: User {} ({}) requesting account deletion", userDetails.getId(), userDetails.getEmail());
        authService.deleteAccount(userDetails.getId(), response);
        log.info("SECURITY: Account deleted for user {} ({})", userDetails.getId(), userDetails.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportData(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (!dataExportRateLimiter.tryAcquire("export:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many export requests. Please try again later.");
        }

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getEmail()));

        Map<String, Object> export = new LinkedHashMap<>();

        // Profile
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("updatedAt", user.getUpdatedAt());
        profile.put("pictureUrl", user.getPictureUrl());
        export.put("profile", profile);

        // Lists with items
        export.put("lists", user.getItemLists().stream().map(this::mapList).toList());

        return ResponseEntity.ok(export);
    }

    @PatchMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UserNotFoundException(userDetails.getEmail()));

        if (user.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot change password for Google-only accounts"));
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    private Map<String, Object> mapList(ItemList list) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", list.getName());
        m.put("description", list.getDescription());
        m.put("category", list.getCategory());
        m.put("customFieldDefinitions", list.getCustomFieldDefinitions());
        m.put("createdAt", list.getCreatedAt());
        m.put("items", list.getItems().stream().map(this::mapItem).toList());
        return m;
    }

    private Map<String, Object> mapItem(Item item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", item.getName());
        m.put("status", item.getStatus().name());
        m.put("stock", item.getStock());
        m.put("customFieldValues", item.getCustomFieldValues());
        m.put("createdAt", item.getCreatedAt());
        m.put("updatedAt", item.getUpdatedAt());
        return m;
    }
}
