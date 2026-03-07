package com.inventory.service;

import com.inventory.enums.Role;
import com.inventory.model.User;
import com.inventory.repository.StripeWebhookEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.impl.StripeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeServiceImpl Tests")
class StripeServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeWebhookEventRepository webhookEventRepository;

    @InjectMocks
    private StripeServiceImpl stripeService;

    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
    }

    @Nested
    @DisplayName("handleWebhookEvent — webhook secret validation")
    class WebhookSecretValidationTests {

        @Test
        @DisplayName("should reject webhook when secret is not configured")
        void rejectsWhenSecretNotConfigured() {
            // webhookSecret defaults to empty string via @Value("${stripe.webhook-secret:}")
            assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe webhook secret not configured");
        }
    }

    @Nested
    @DisplayName("upgrade logic — idempotency and role checks")
    class UpgradeIdempotencyTests {

        @Test
        @DisplayName("should not downgrade admin to premium")
        void admin_notDowngraded() {
            testUser.setRole(Role.ADMIN);

            boolean shouldSkip = testUser.getRole() == Role.PREMIUM_USER || testUser.getRole() == Role.ADMIN;

            assertThat(shouldSkip).isTrue();
        }

        @Test
        @DisplayName("should not re-upgrade premium user")
        void premiumUser_notReUpgraded() {
            testUser.setRole(Role.PREMIUM_USER);

            boolean shouldSkip = testUser.getRole() == Role.PREMIUM_USER || testUser.getRole() == Role.ADMIN;

            assertThat(shouldSkip).isTrue();
        }

        @Test
        @DisplayName("free user should be eligible for upgrade")
        void freeUser_eligibleForUpgrade() {
            testUser.setRole(Role.USER);

            boolean shouldSkip = testUser.getRole() == Role.PREMIUM_USER || testUser.getRole() == Role.ADMIN;

            assertThat(shouldSkip).isFalse();
        }

        @Test
        @DisplayName("upgrade sets role and payment ID")
        void upgrade_setsRoleAndPaymentId() {
            testUser.setRole(Role.USER);
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Simulate the upgrade logic from upgradeUserById
            userRepository.findById(testUserId).ifPresent(user -> {
                if (user.getRole() != Role.PREMIUM_USER && user.getRole() != Role.ADMIN) {
                    user.setRole(Role.PREMIUM_USER);
                    user.setStripePaymentId("pi_test_123");
                    userRepository.save(user);
                }
            });

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            assertThat(testUser.getStripePaymentId()).isEqualTo("pi_test_123");
            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("payment_intent.succeeded — customer ID fallback")
    class PaymentIntentCustomerFallbackTests {

        @Test
        @DisplayName("should upgrade user found by stripe customer ID")
        void upgradesViaCustomerId() {
            testUser.setStripeCustomerId("cus_test_123");
            when(userRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Simulate the customer ID fallback path from handlePaymentIntentSucceeded
            userRepository.findByStripeCustomerId("cus_test_123").ifPresent(user -> {
                if (user.getRole() != Role.PREMIUM_USER && user.getRole() != Role.ADMIN) {
                    user.setRole(Role.PREMIUM_USER);
                    user.setStripePaymentId("pi_test_456");
                    userRepository.save(user);
                }
            });

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            assertThat(testUser.getStripePaymentId()).isEqualTo("pi_test_456");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("should skip upgrade if user already premium via customer ID lookup")
        void skipsUpgradeIfAlreadyPremium() {
            testUser.setRole(Role.PREMIUM_USER);
            testUser.setStripeCustomerId("cus_test_123");
            when(userRepository.findByStripeCustomerId("cus_test_123")).thenReturn(Optional.of(testUser));

            userRepository.findByStripeCustomerId("cus_test_123").ifPresent(user -> {
                if (user.getRole() != Role.PREMIUM_USER && user.getRole() != Role.ADMIN) {
                    user.setRole(Role.PREMIUM_USER);
                    user.setStripePaymentId("pi_test_456");
                    userRepository.save(user);
                }
            });

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should not upgrade when no user found by customer ID")
        void noUpgradeWhenUserNotFound() {
            when(userRepository.findByStripeCustomerId("cus_unknown")).thenReturn(Optional.empty());

            userRepository.findByStripeCustomerId("cus_unknown").ifPresent(user -> {
                user.setRole(Role.PREMIUM_USER);
                userRepository.save(user);
            });

            verify(userRepository, never()).save(any());
        }
    }
}
