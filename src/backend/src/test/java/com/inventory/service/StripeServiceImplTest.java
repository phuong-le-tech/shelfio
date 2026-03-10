package com.inventory.service;

import com.inventory.enums.Role;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.User;
import com.inventory.repository.StripeWebhookEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.impl.StripeServiceImpl;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Dispute;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final Long PRICE_AMOUNT = 200L;
    private static final String PRICE_CURRENCY = "eur";
    private static final String FRONTEND_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
    }

    @AfterEach
    void tearDown() {
        Stripe.apiKey = null;
    }

    private void setDefaultFields() {
        Stripe.apiKey = "sk_test_dummy";
        ReflectionTestUtils.setField(stripeService, "webhookSecret", WEBHOOK_SECRET);
        ReflectionTestUtils.setField(stripeService, "priceAmount", PRICE_AMOUNT);
        ReflectionTestUtils.setField(stripeService, "priceCurrency", PRICE_CURRENCY);
        ReflectionTestUtils.setField(stripeService, "frontendUrl", FRONTEND_URL);
    }

    private void invokeValidatePriceConfig() throws Exception {
        Method method = StripeServiceImpl.class.getDeclaredMethod("validatePriceConfig");
        method.setAccessible(true);
        try {
            method.invoke(stripeService);
        } catch (InvocationTargetException e) {
            throw (Exception) e.getCause();
        }
    }

    // -----------------------------------------------------------------------
    // handleWebhookEvent — webhook secret validation and event parsing
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("handleWebhookEvent — webhook secret and signature")
    class WebhookEventParsingTests {

        @Test
        @DisplayName("1. throws IllegalStateException when webhookSecret is null")
        void throwsWhenSecretNull() {
            ReflectionTestUtils.setField(stripeService, "webhookSecret", null);

            assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe webhook secret not configured");
        }

        @Test
        @DisplayName("2. throws IllegalStateException when webhookSecret is blank")
        void throwsWhenSecretBlank() {
            ReflectionTestUtils.setField(stripeService, "webhookSecret", "   ");

            assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "sig"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe webhook secret not configured");
        }

        @Test
        @DisplayName("3. throws UnauthorizedException on SignatureVerificationException")
        void throwsOnSignatureVerification() {
            setDefaultFields();

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new SignatureVerificationException("bad sig", "sig_header"));

                assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "bad_sig"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid webhook signature");
            }
        }

        @Test
        @DisplayName("4. throws UnauthorizedException on generic parse exception")
        void throwsOnGenericParseException() {
            setDefaultFields();

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("parse error"));

                assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "sig"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Invalid webhook payload");
            }
        }

        @Test
        @DisplayName("5. returns silently on duplicate event (insertIfNotExists returns 0)")
        void returnsSilentlyOnDuplicateEvent() {
            setDefaultFields();

            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_dup");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(0);

                stripeService.handleWebhookEvent("{}", "sig");

                verify(webhookEventRepository).insertIfNotExists(any(UUID.class), anyString(), anyString());
                verifyNoInteractions(userRepository);
            }
        }

        @Test
        @DisplayName("6. handles unhandled event type without error")
        void handlesUnhandledEventType() {
            setDefaultFields();

            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_unknown");
            lenient().when(event.getType()).thenReturn("some.unknown.event");

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                stripeService.handleWebhookEvent("{}", "sig");

                verify(webhookEventRepository).insertIfNotExists(any(UUID.class), anyString(), anyString());
                verifyNoInteractions(userRepository);
            }
        }
    }

    // -----------------------------------------------------------------------
    // checkout.session.completed handler
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("checkout.session.completed")
    class CheckoutSessionCompletedTests {

        private void executeWebhookWithEvent(Event event) {
            setDefaultFields();
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                stripeService.handleWebhookEvent("{}", "sig");
            }
        }

        @Test
        @DisplayName("7. upgrades user when session is paid with valid amount and metadata")
        void upgradesUserOnValidCheckout() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_1");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_123");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn("cus_123");
            when(session.getPaymentIntent()).thenReturn("pi_test_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            assertThat(testUser.getStripePaymentId()).isEqualTo("pi_test_123");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("8. returns early when session not paid")
        void returnsEarlyWhenNotPaid() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_2");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_unpaid");
            when(session.getPaymentStatus()).thenReturn("unpaid");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("9. returns early when amount is null")
        void returnsEarlyWhenAmountNull() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_3");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_null_amount");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(null);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("10. returns early when amount is below price")
        void returnsEarlyWhenAmountBelowPrice() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_4");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_low_amount");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(100L);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("11. returns early when metadata is null")
        void returnsEarlyWhenMetadataNull() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_5");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_no_meta");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getMetadata()).thenReturn(null);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("12. returns early when userId missing from metadata")
        void returnsEarlyWhenUserIdMissingFromMetadata() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_6");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_no_uid");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getMetadata()).thenReturn(Map.of("other", "value"));

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("13. returns early when userId is invalid UUID format")
        void returnsEarlyWhenUserIdInvalidFormat() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_7");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_bad_uuid");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getMetadata()).thenReturn(Map.of("userId", "not-a-uuid"));

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("14. returns early on customer ID mismatch")
        void returnsEarlyOnCustomerIdMismatch() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_8");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_mismatch");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn("cus_session_999");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            testUser.setStripeCustomerId("cus_user_111");
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("15. skips cross-verification when session has no customer ID")
        void skipsCrossVerificationWhenNoCustomerId() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_9");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_no_cust");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn(null);
            when(session.getPaymentIntent()).thenReturn("pi_test_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findById(any());
            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("16. skips upgrade when user already premium")
        void skipsUpgradeWhenAlreadyPremium() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_10");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_premium");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn(null);
            when(session.getPaymentIntent()).thenReturn("pi_test_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            testUser.setRole(Role.PREMIUM_USER);
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).save(any());
            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
        }

        @Test
        @DisplayName("17. skips upgrade when user is admin")
        void skipsUpgradeWhenAdmin() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_11");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_admin");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn(null);
            when(session.getPaymentIntent()).thenReturn("pi_test_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            testUser.setRole(Role.ADMIN);
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).save(any());
            assertThat(testUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("18. logs error when user not found for upgrade")
        void logsErrorWhenUserNotFound() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_12");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            Session session = mock(Session.class);
            when(session.getId()).thenReturn("cs_test_not_found");
            when(session.getPaymentStatus()).thenReturn("paid");
            when(session.getAmountTotal()).thenReturn(200L);
            when(session.getCurrency()).thenReturn("eur");
            when(session.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));
            when(session.getCustomer()).thenReturn(null);
            when(session.getPaymentIntent()).thenReturn("pi_test_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.empty());

            executeWebhookWithEvent(event);

            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // payment_intent.succeeded handler
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("payment_intent.succeeded")
    class PaymentIntentSucceededTests {

        private void executeWebhookWithEvent(Event event) {
            setDefaultFields();
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                stripeService.handleWebhookEvent("{}", "sig");
            }
        }

        @Test
        @DisplayName("19. upgrades user via metadata userId when present")
        void upgradesUserViaMetadata() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_pi_1");
            lenient().when(event.getType()).thenReturn("payment_intent.succeeded");

            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn("pi_test_200");
            when(pi.getAmount()).thenReturn(200L);
            when(pi.getCurrency()).thenReturn("eur");
            when(pi.getMetadata()).thenReturn(Map.of("userId", testUserId.toString()));

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));

            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            assertThat(testUser.getStripePaymentId()).isEqualTo("pi_test_200");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("20. falls back to customer ID when no metadata userId")
        void fallsBackToCustomerId() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_pi_2");
            lenient().when(event.getType()).thenReturn("payment_intent.succeeded");

            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn("pi_test_fallback");
            when(pi.getAmount()).thenReturn(200L);
            when(pi.getCurrency()).thenReturn("eur");
            when(pi.getMetadata()).thenReturn(Map.of());
            when(pi.getCustomer()).thenReturn("cus_fallback");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));

            when(userRepository.findByStripeCustomerIdWithLock("cus_fallback")).thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.PREMIUM_USER);
            assertThat(testUser.getStripePaymentId()).isEqualTo("pi_test_fallback");
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("21. skips when amount below threshold")
        void skipsWhenAmountBelowThreshold() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_pi_3");
            lenient().when(event.getType()).thenReturn("payment_intent.succeeded");

            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn("pi_test_low");
            when(pi.getAmount()).thenReturn(100L);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("22. skips when amount is null")
        void skipsWhenAmountNull() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_pi_4");
            lenient().when(event.getType()).thenReturn("payment_intent.succeeded");

            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn("pi_test_null");
            when(pi.getAmount()).thenReturn(null);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("23. no upgrade when customer ID is null and no metadata userId")
        void noUpgradeWhenNoCustomerAndNoMetadata() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_pi_5");
            lenient().when(event.getType()).thenReturn("payment_intent.succeeded");

            PaymentIntent pi = mock(PaymentIntent.class);
            when(pi.getId()).thenReturn("pi_test_orphan");
            when(pi.getAmount()).thenReturn(200L);
            when(pi.getMetadata()).thenReturn(Map.of());
            when(pi.getCustomer()).thenReturn(null);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(pi));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByIdWithLock(any());
            verify(userRepository, never()).findByStripeCustomerIdWithLock(any());
            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // charge.refunded handler
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("charge.refunded")
    class ChargeRefundedTests {

        private void executeWebhookWithEvent(Event event) {
            setDefaultFields();
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                stripeService.handleWebhookEvent("{}", "sig");
            }
        }

        @Test
        @DisplayName("24. downgrades premium user to USER")
        void downgradesPremiumUser() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_ref_1");
            lenient().when(event.getType()).thenReturn("charge.refunded");

            Charge charge = mock(Charge.class);
            when(charge.getId()).thenReturn("ch_test_refund");
            when(charge.getCustomer()).thenReturn("cus_refund_123");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(charge));

            testUser.setRole(Role.PREMIUM_USER);
            when(userRepository.findByStripeCustomerIdWithLock("cus_refund_123"))
                .thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.USER);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("25. skips downgrade when user is not premium")
        void skipsDowngradeWhenNotPremium() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_ref_2");
            lenient().when(event.getType()).thenReturn("charge.refunded");

            Charge charge = mock(Charge.class);
            when(charge.getId()).thenReturn("ch_test_not_premium");
            when(charge.getCustomer()).thenReturn("cus_not_premium");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(charge));

            testUser.setRole(Role.USER);
            when(userRepository.findByStripeCustomerIdWithLock("cus_not_premium"))
                .thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.USER);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("26. logs error when no customer ID on charge")
        void logsErrorWhenNoCustomerId() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_ref_3");
            lenient().when(event.getType()).thenReturn("charge.refunded");

            Charge charge = mock(Charge.class);
            when(charge.getId()).thenReturn("ch_test_no_cust");
            when(charge.getCustomer()).thenReturn(null);

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(charge));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByStripeCustomerIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("27. logs error when no user found by customer ID")
        void logsErrorWhenNoUserFoundByCustomerId() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_ref_4");
            lenient().when(event.getType()).thenReturn("charge.refunded");

            Charge charge = mock(Charge.class);
            when(charge.getId()).thenReturn("ch_test_no_user");
            when(charge.getCustomer()).thenReturn("cus_not_found");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(charge));

            when(userRepository.findByStripeCustomerIdWithLock("cus_not_found"))
                .thenReturn(Optional.empty());

            executeWebhookWithEvent(event);

            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // charge.dispute.created handler
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("charge.dispute.created")
    class ChargeDisputedTests {

        private void executeWebhookWithEvent(Event event) {
            setDefaultFields();
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                stripeService.handleWebhookEvent("{}", "sig");
            }
        }

        @Test
        @DisplayName("28. downgrades premium user via payment intent")
        void downgradesPremiumUserViaPaymentIntent() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_disp_1");
            lenient().when(event.getType()).thenReturn("charge.dispute.created");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);

            Dispute dispute = mock(Dispute.class);
            when(dispute.getPaymentIntent()).thenReturn("pi_disputed");
            when(deserializer.getObject()).thenReturn(Optional.of(dispute));

            testUser.setRole(Role.PREMIUM_USER);
            when(userRepository.findByStripePaymentIdWithLock("pi_disputed"))
                .thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.USER);
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("29. skips downgrade when user is not premium for dispute")
        void skipsDowngradeWhenNotPremiumForDispute() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_disp_2");
            lenient().when(event.getType()).thenReturn("charge.dispute.created");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);

            Dispute dispute = mock(Dispute.class);
            when(dispute.getPaymentIntent()).thenReturn("pi_dispute_not_premium");
            when(deserializer.getObject()).thenReturn(Optional.of(dispute));

            testUser.setRole(Role.USER);
            when(userRepository.findByStripePaymentIdWithLock("pi_dispute_not_premium"))
                .thenReturn(Optional.of(testUser));

            executeWebhookWithEvent(event);

            assertThat(testUser.getRole()).isEqualTo(Role.USER);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("30. no action when dispute has no payment intent")
        void noActionWhenDisputeHasNoPaymentIntent() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_disp_3");
            lenient().when(event.getType()).thenReturn("charge.dispute.created");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);

            Dispute dispute = mock(Dispute.class);
            when(dispute.getPaymentIntent()).thenReturn(null);
            when(deserializer.getObject()).thenReturn(Optional.of(dispute));

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByStripePaymentIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("31. returns early when deserializer cannot deserialize dispute")
        void returnsEarlyWhenCannotDeserializeDispute() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_disp_4");
            lenient().when(event.getType()).thenReturn("charge.dispute.created");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            executeWebhookWithEvent(event);

            verify(userRepository, never()).findByStripePaymentIdWithLock(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("32. no user found for disputed payment intent")
        void noUserFoundForDisputedPaymentIntent() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_disp_5");
            lenient().when(event.getType()).thenReturn("charge.dispute.created");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);

            Dispute dispute = mock(Dispute.class);
            when(dispute.getPaymentIntent()).thenReturn("pi_not_found");
            when(deserializer.getObject()).thenReturn(Optional.of(dispute));

            when(userRepository.findByStripePaymentIdWithLock("pi_not_found"))
                .thenReturn(Optional.empty());

            executeWebhookWithEvent(event);

            verify(userRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // createCheckoutSession
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("createCheckoutSession")
    class CreateCheckoutSessionTests {

        @Test
        @DisplayName("33. returns URL when existing stripe customer ID exists")
        void returnsUrlWhenExistingCustomer() {
            setDefaultFields();
            testUser.setStripeCustomerId("cus_existing");
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                Session session = mock(Session.class);
                when(session.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_abc");
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

                String url = stripeService.createCheckoutSession(testUser);

                assertThat(url).isEqualTo("https://checkout.stripe.com/pay/cs_test_abc");
                verify(userRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("34. creates new Stripe customer when none exists")
        void createsNewStripeCustomer() {
            setDefaultFields();
            testUser.setStripeCustomerId(null);
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class);
                 MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {

                Customer customer = mock(Customer.class);
                when(customer.getId()).thenReturn("cus_new_123");
                customerMock.when(() -> Customer.create(any(CustomerCreateParams.class))).thenReturn(customer);

                Session session = mock(Session.class);
                when(session.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_new");
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

                String url = stripeService.createCheckoutSession(testUser);

                assertThat(url).isEqualTo("https://checkout.stripe.com/pay/cs_test_new");
                assertThat(testUser.getStripeCustomerId()).isEqualTo("cus_new_123");
                verify(userRepository).save(testUser);
            }
        }

        @Test
        @DisplayName("35. throws RuntimeException when Session.create returns null URL")
        void throwsWhenNullUrl() {
            setDefaultFields();
            testUser.setStripeCustomerId("cus_existing");
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                Session session = mock(Session.class);
                when(session.getUrl()).thenReturn(null);
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

                assertThatThrownBy(() -> stripeService.createCheckoutSession(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Payment service unavailable");
            }
        }

        @Test
        @DisplayName("36. throws RuntimeException when Session.create returns non-Stripe URL")
        void throwsWhenNonStripeUrl() {
            setDefaultFields();
            testUser.setStripeCustomerId("cus_existing");
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                Session session = mock(Session.class);
                when(session.getUrl()).thenReturn("https://evil.com/checkout");
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

                assertThatThrownBy(() -> stripeService.createCheckoutSession(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Payment service unavailable");
            }
        }

        @Test
        @DisplayName("37. throws RuntimeException when StripeException occurs")
        void throwsWhenStripeExceptionOccurs() {
            setDefaultFields();
            testUser.setStripeCustomerId("cus_existing");
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));

            try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
                sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(mock(StripeException.class));

                assertThatThrownBy(() -> stripeService.createCheckoutSession(testUser))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Payment service unavailable");
            }
        }

        @Test
        @DisplayName("38. throws RuntimeException when user not found by ID")
        void throwsWhenUserNotFoundById() {
            setDefaultFields();
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> stripeService.createCheckoutSession(testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
        }

        @Test
        @DisplayName("39. throws IllegalStateException when Stripe API key is not configured")
        void throwsWhenStripeNotConfigured() {
            Stripe.apiKey = null;
            ReflectionTestUtils.setField(stripeService, "frontendUrl", FRONTEND_URL);

            assertThatThrownBy(() -> stripeService.createCheckoutSession(testUser))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment features are not available");
        }
    }

    // -----------------------------------------------------------------------
    // deserializeEventObject
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("deserializeEventObject")
    class DeserializeEventObjectTests {

        @Test
        @DisplayName("40. throws RuntimeException on API version mismatch")
        void throwsOnApiVersionMismatch() {
            Event event = mock(Event.class);
            lenient().when(event.getId()).thenReturn("evt_mismatch");
            lenient().when(event.getType()).thenReturn("checkout.session.completed");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            setDefaultFields();
            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                webhookMock.when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
                    .thenReturn(event);
                when(webhookEventRepository.insertIfNotExists(any(UUID.class), anyString(), anyString()))
                    .thenReturn(1);

                assertThatThrownBy(() -> stripeService.handleWebhookEvent("{}", "sig"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot safely deserialize Stripe event");
            }
        }
    }

    // -----------------------------------------------------------------------
    // validatePriceConfig
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("validatePriceConfig")
    class ValidatePriceConfigTests {

        @Test
        @DisplayName("41. throws IllegalStateException when priceAmount is null")
        void throwsWhenPriceAmountNull() {
            ReflectionTestUtils.setField(stripeService, "priceAmount", null);

            assertThatThrownBy(() -> invokeValidatePriceConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stripe.price-amount must be positive");
        }

        @Test
        @DisplayName("42. throws IllegalStateException when priceAmount is zero")
        void throwsWhenPriceAmountZero() {
            ReflectionTestUtils.setField(stripeService, "priceAmount", 0L);

            assertThatThrownBy(() -> invokeValidatePriceConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stripe.price-amount must be positive, got: 0");
        }

        @Test
        @DisplayName("43. throws IllegalStateException when priceAmount is negative")
        void throwsWhenPriceAmountNegative() {
            ReflectionTestUtils.setField(stripeService, "priceAmount", -100L);

            assertThatThrownBy(() -> invokeValidatePriceConfig())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stripe.price-amount must be positive, got: -100");
        }
    }
}
