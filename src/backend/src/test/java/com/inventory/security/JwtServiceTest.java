package com.inventory.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private static final String TEST_SECRET = "test-secret-key-for-testing-purposes-only-minimum-256-bits";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 86400000L);
    }

    @Nested
    @DisplayName("validateSecret")
    class ValidateSecretTests {

        @Test
        @DisplayName("should accept valid secret (32+ characters)")
        void validSecret_noException() {
            jwtService.validateSecret();
            // No exception means success
        }

        @Test
        @DisplayName("should reject null secret")
        void nullSecret_throwsException() {
            ReflectionTestUtils.setField(jwtService, "jwtSecret", null);
            assertThatThrownBy(() -> jwtService.validateSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_SECRET");
        }

        @Test
        @DisplayName("should reject short secret")
        void shortSecret_throwsException() {
            ReflectionTestUtils.setField(jwtService, "jwtSecret", "too-short");
            assertThatThrownBy(() -> jwtService.validateSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("too short");
        }

        @Test
        @DisplayName("should reject blank secret")
        void blankSecret_throwsException() {
            ReflectionTestUtils.setField(jwtService, "jwtSecret", "                                ");
            assertThatThrownBy(() -> jwtService.validateSecret())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("generateToken and parseToken")
    class TokenGenerationTests {

        @Test
        @DisplayName("should generate token with correct claims")
        void generateToken_containsCorrectClaims() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "test@example.com", "USER");

            Claims claims = jwtService.parseToken(token);

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo("test@example.com");
            assertThat(claims.get("role", String.class)).isEqualTo("USER");
            assertThat(claims.getIssuedAt()).isNotNull();
            assertThat(claims.getExpiration()).isAfter(new Date());
        }

        @Test
        @DisplayName("should extract userId from token")
        void getUserIdFromToken_returnsCorrectId() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "test@example.com", "ADMIN");

            UUID extractedId = jwtService.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(userId);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("should return true for valid token")
        void validToken_returnsTrue() {
            String token = jwtService.generateToken(UUID.randomUUID(), "test@example.com", "USER");

            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("should return false for expired token")
        void expiredToken_returnsFalse() {
            // Create service with 0ms expiration
            ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 0L);
            String token = jwtService.generateToken(UUID.randomUUID(), "test@example.com", "USER");

            assertThat(jwtService.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("should return false for malformed token")
        void malformedToken_returnsFalse() {
            assertThat(jwtService.validateToken("not.a.valid.token")).isFalse();
        }

        @Test
        @DisplayName("should return false for token signed with different key")
        void wrongKey_returnsFalse() {
            // Generate a token with a different secret
            String differentSecret = "a-completely-different-secret-key-for-signing-tokens!!";
            String forgedToken = Jwts.builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("email", "hacker@evil.com")
                    .claim("role", "ADMIN")
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 86400000))
                    .signWith(Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8)))
                    .compact();

            assertThat(jwtService.validateToken(forgedToken)).isFalse();
        }

        @Test
        @DisplayName("should return false for empty token")
        void emptyToken_returnsFalse() {
            assertThat(jwtService.validateToken("")).isFalse();
        }
    }
}
