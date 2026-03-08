package com.inventory.security;

import com.inventory.enums.Role;
import com.inventory.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails Tests")
class CustomUserDetailsTest {

    @Nested
    @DisplayName("User entity constructor")
    class UserEntityConstructorTests {

        @Test
        @DisplayName("should build from User entity with correct authorities")
        void buildsFromUserEntity() {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail("test@example.com");
            user.setPassword("encoded-password");
            user.setRole(Role.ADMIN);
            user.setEnabled(true);

            CustomUserDetails details = new CustomUserDetails(user);

            assertThat(details.getId()).isEqualTo(user.getId());
            assertThat(details.getEmail()).isEqualTo("test@example.com");
            assertThat(details.getUsername()).isEqualTo("test@example.com");
            assertThat(details.getPassword()).isEqualTo("encoded-password");
            assertThat(details.isEnabled()).isTrue();
            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should reflect disabled status from User entity")
        void reflectsDisabledStatus() {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail("disabled@example.com");
            user.setRole(Role.USER);
            user.setEnabled(false);

            CustomUserDetails details = new CustomUserDetails(user);

            assertThat(details.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("JWT claims constructor")
    class JwtClaimsConstructorTests {

        @Test
        @DisplayName("should build from JWT claims with ROLE_ prefix")
        void buildsFromJwtClaims() {
            UUID id = UUID.randomUUID();

            CustomUserDetails details = new CustomUserDetails(id, "jwt@example.com", "PREMIUM_USER");

            assertThat(details.getId()).isEqualTo(id);
            assertThat(details.getEmail()).isEqualTo("jwt@example.com");
            assertThat(details.getUsername()).isEqualTo("jwt@example.com");
            assertThat(details.getPassword()).isNull();
            assertThat(details.isEnabled()).isTrue();
            assertThat(details.getAuthorities()).hasSize(1);
            assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_PREMIUM_USER");
        }
    }

    @Nested
    @DisplayName("UserDetails interface methods")
    class UserDetailsInterfaceTests {

        @Test
        @DisplayName("should always return true for account non-expired/non-locked/credentials-non-expired")
        void accountStatusAlwaysTrue() {
            CustomUserDetails details = new CustomUserDetails(UUID.randomUUID(), "test@example.com", "USER");

            assertThat(details.isAccountNonExpired()).isTrue();
            assertThat(details.isAccountNonLocked()).isTrue();
            assertThat(details.isCredentialsNonExpired()).isTrue();
        }
    }
}
