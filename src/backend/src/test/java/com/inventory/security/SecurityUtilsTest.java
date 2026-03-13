package com.inventory.security;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

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
    @DisplayName("getCurrentUserId")
    class GetCurrentUserIdTests {

        @Test
        @DisplayName("should return user ID when authenticated")
        void returnsUserIdWhenAuthenticated() {
            UUID userId = UUID.randomUUID();
            setAuthentication(userId, "user@example.com", "USER");

            Optional<UUID> result = securityUtils.getCurrentUserId();

            assertThat(result).contains(userId);
        }

        @Test
        @DisplayName("should return empty when no authentication")
        void emptyWhenNoAuthentication() {
            Optional<UUID> result = securityUtils.getCurrentUserId();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for anonymous authentication")
        void emptyForAnonymousAuthentication() {
            var auth = new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<UUID> result = securityUtils.getCurrentUserId();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isAdmin")
    class IsAdminTests {

        @Test
        @DisplayName("should return true for ADMIN role")
        void trueForAdmin() {
            setAuthentication(UUID.randomUUID(), "admin@example.com", "ADMIN");

            assertThat(securityUtils.isAdmin()).isTrue();
        }

        @Test
        @DisplayName("should return false for USER role")
        void falseForUser() {
            setAuthentication(UUID.randomUUID(), "user@example.com", "USER");

            assertThat(securityUtils.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("should return false for PREMIUM_USER role")
        void falseForPremiumUser() {
            setAuthentication(UUID.randomUUID(), "premium@example.com", "PREMIUM_USER");

            assertThat(securityUtils.isAdmin()).isFalse();
        }

        @Test
        @DisplayName("should return false when no authentication")
        void falseWhenNoAuth() {
            assertThat(securityUtils.isAdmin()).isFalse();
        }
    }

}
