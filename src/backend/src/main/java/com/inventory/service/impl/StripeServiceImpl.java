package com.inventory.service.impl;

import com.inventory.enums.Role;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.StripeWebhookEvent;
import com.inventory.model.User;
import com.inventory.repository.StripeWebhookEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.service.IStripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeServiceImpl implements IStripeService {

    private final UserRepository userRepository;
    private final StripeWebhookEventRepository webhookEventRepository;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.price-amount:200}")
    private Long priceAmount;

    @jakarta.annotation.PostConstruct
    void validatePriceConfig() {
        if (priceAmount == null || priceAmount <= 0) {
            throw new IllegalStateException("stripe.price-amount must be positive, got: " + priceAmount);
        }
    }

    @Value("${stripe.price-currency:eur}")
    private String priceCurrency;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public String createCheckoutSession(User user) {
        try {
            String customerId = getOrCreateStripeCustomer(user);

            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setCustomer(customerId)
                .setSuccessUrl(frontendUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl + "/payment/cancel")
                .putMetadata("userId", user.getId().toString())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("userId", user.getId().toString())
                    .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(priceCurrency)
                        .setUnitAmount(priceAmount)
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Premium Upgrade")
                            .setDescription("Unlimited list creation — one-time payment")
                            .build())
                        .build())
                    .build())
                .build();

            Session session = Session.create(params);
            String url = session.getUrl();
            if (url == null || !url.startsWith("https://checkout.stripe.com/")) {
                log.error("Unexpected Stripe checkout URL: {}", url);
                throw new RuntimeException("Payment service unavailable");
            }
            return url;
        } catch (StripeException e) {
            log.error("Stripe checkout failed for user {}: code={}, message={}", user.getId(), e.getCode(), e.getMessage());
            throw new RuntimeException("Payment service unavailable");
        }
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("STRIPE_WEBHOOK_SECRET is not configured — cannot verify webhook signatures");
            throw new IllegalStateException("Stripe webhook secret not configured");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            Sentry.captureMessage("Invalid Stripe webhook signature attempt");
            throw new UnauthorizedException("Invalid webhook signature");
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event: {}", e.getMessage());
            throw new UnauthorizedException("Invalid webhook payload");
        }

        // Idempotency: insert first, rely on unique constraint to reject duplicates (no TOCTOU race)
        try {
            webhookEventRepository.saveAndFlush(new StripeWebhookEvent(event.getId(), event.getType()));
        } catch (DataIntegrityViolationException e) {
            log.info("Stripe event {} already processed, skipping", event.getId());
            return;
        }

        log.info("Stripe webhook received: type={}, id={}, apiVersion={}", event.getType(), event.getId(), event.getApiVersion());

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            case "charge.dispute.created" -> handleChargeDisputed(event);
            default -> log.debug("Unhandled event type: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        log.info("Processing checkout.session.completed event: {}", event.getId());
        Session session = deserializeEventObject(event, Session.class);

        log.info("Session {}: paymentStatus={}, amountTotal={}, metadata={}",
            session.getId(), session.getPaymentStatus(), session.getAmountTotal(), session.getMetadata());

        if (!"paid".equals(session.getPaymentStatus())) {
            log.warn("Checkout session {} not paid, status: {}", session.getId(), session.getPaymentStatus());
            Sentry.captureMessage("Stripe webhook: unpaid session " + session.getId());
            return;
        }

        if (session.getAmountTotal() == null || session.getAmountTotal() < priceAmount) {
            log.error("Invalid payment amount for session {}: got={}, expected={}",
                session.getId(), session.getAmountTotal(), priceAmount);
            Sentry.captureMessage("Stripe webhook: invalid amount for session " + session.getId());
            return;
        }

        Optional<UUID> userId = extractUserIdFromMetadata(session.getMetadata(), "session " + session.getId());
        if (userId.isEmpty()) {
            return;
        }

        if (!verifyCustomerIdMatch(session.getCustomer(), userId.get(), session.getId())) {
            return;
        }

        upgradeUserById(userId.get(), session.getPaymentIntent());
    }

    private void handlePaymentIntentSucceeded(Event event) {
        log.info("Processing payment_intent.succeeded event: {}", event.getId());
        PaymentIntent paymentIntent = deserializeEventObject(event, PaymentIntent.class);

        log.info("PaymentIntent {}: amount={}, currency={}, customer={}, metadata={}",
            paymentIntent.getId(), paymentIntent.getAmount(), paymentIntent.getCurrency(),
            paymentIntent.getCustomer(), paymentIntent.getMetadata());

        if (paymentIntent.getAmount() == null || paymentIntent.getAmount() < priceAmount) {
            log.debug("PaymentIntent {} amount {} below premium price {}, skipping",
                paymentIntent.getId(), paymentIntent.getAmount(), priceAmount);
            return;
        }

        Optional<UUID> userId = extractUserIdFromMetadata(paymentIntent.getMetadata(),
            "PaymentIntent " + paymentIntent.getId());
        if (userId.isPresent()) {
            upgradeUserById(userId.get(), paymentIntent.getId());
            return;
        }

        upgradeByStripeCustomerId(paymentIntent.getCustomer(), paymentIntent.getId());
    }

    private void upgradeUserById(UUID userId, String paymentIntentId) {
        userRepository.findByIdWithLock(userId).ifPresentOrElse(user -> {
            if (user.getRole() == Role.PREMIUM_USER || user.getRole() == Role.ADMIN) {
                log.info("User {} already premium/admin, skipping upgrade", userId);
                return;
            }

            user.setRole(Role.PREMIUM_USER);
            user.setStripePaymentId(paymentIntentId);
            userRepository.save(user);
            log.info("User {} upgraded to PREMIUM_USER", userId);
        }, () -> {
            log.error("User {} not found for Stripe webhook", userId);
            Sentry.captureMessage("Stripe webhook: user not found " + userId);
        });
    }

    private void handleChargeRefunded(Event event) {
        log.info("Processing charge.refunded event: {}", event.getId());
        Charge charge = deserializeEventObject(event, Charge.class);

        String customerId = charge.getCustomer();
        if (customerId == null) {
            log.warn("Refunded charge {} has no customer ID, cannot downgrade", charge.getId());
            Sentry.captureMessage("Stripe webhook: refunded charge without customer " + charge.getId());
            return;
        }

        downgradeUserByCustomerId(customerId, "charge.refunded", charge.getId());
    }

    private void handleChargeDisputed(Event event) {
        log.info("Processing charge.dispute.created event: {}", event.getId());
        Sentry.captureMessage("Stripe dispute created: " + event.getId());

        // Dispute data object contains the charge ID; retrieve customer from nested charge info
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            log.error("Cannot deserialize dispute event {}", event.getId());
            return;
        }

        StripeObject obj = deserializer.getObject().get();
        // Stripe Dispute has a getCharge() but it returns an ID string.
        // We need the Charge object's customer. Try to extract via the raw JSON metadata.
        if (obj instanceof com.stripe.model.Dispute dispute) {
            // The dispute object may have an expanded charge with customer info
            // For safety, look up via payment intent metadata if available
            String paymentIntentId = dispute.getPaymentIntent();
            if (paymentIntentId != null) {
                userRepository.findByStripePaymentIdWithLock(paymentIntentId)
                    .ifPresentOrElse(
                        user -> {
                            if (user.getRole() == Role.PREMIUM_USER) {
                                user.setRole(Role.USER);
                                userRepository.save(user);
                                log.info("User {} downgraded due to dispute on PaymentIntent {}", user.getId(), paymentIntentId);
                            }
                        },
                        () -> log.warn("No user found for disputed PaymentIntent {}", paymentIntentId)
                    );
            } else {
                log.warn("Dispute {} has no PaymentIntent reference", event.getId());
            }
        }
    }

    private void downgradeUserByCustomerId(String customerId, String reason, String referenceId) {
        userRepository.findByStripeCustomerIdWithLock(customerId).ifPresentOrElse(user -> {
            if (user.getRole() != Role.PREMIUM_USER) {
                log.info("User {} is not premium (role={}), skipping downgrade for {}", user.getId(), user.getRole(), reason);
                return;
            }

            user.setRole(Role.USER);
            userRepository.save(user);
            log.info("User {} downgraded to USER due to {} (ref: {})", user.getId(), reason, referenceId);
        }, () -> {
            log.error("No user found with stripeCustomerId={} for {} (ref: {})", customerId, reason, referenceId);
            Sentry.captureMessage("Stripe webhook: no user for customer " + customerId + " during " + reason);
        });
    }

    private Optional<UUID> extractUserIdFromMetadata(Map<String, String> metadata, String context) {
        if (metadata == null) {
            log.error("Stripe webhook missing metadata for {}", context);
            Sentry.captureMessage("Stripe webhook: missing metadata for " + context);
            return Optional.empty();
        }
        String userIdStr = metadata.get("userId");
        if (userIdStr == null) {
            log.error("Stripe webhook missing userId metadata for {}", context);
            Sentry.captureMessage("Stripe webhook: missing userId for " + context);
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(userIdStr));
        } catch (IllegalArgumentException e) {
            log.error("Invalid userId format in webhook metadata: {}", userIdStr);
            Sentry.captureMessage("Stripe webhook: invalid userId format: " + userIdStr);
            return Optional.empty();
        }
    }

    private boolean verifyCustomerIdMatch(String sessionCustomerId, UUID userId, String sessionId) {
        if (sessionCustomerId == null) {
            log.warn("Session {} has no customer ID — skipping cross-verification", sessionId);
            return true;
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent() && userOpt.get().getStripeCustomerId() != null
                && !userOpt.get().getStripeCustomerId().equals(sessionCustomerId)) {
            log.error("Customer ID mismatch for session {}: userId={}, session customer={}, user customer={}",
                sessionId, userId, sessionCustomerId, userOpt.get().getStripeCustomerId());
            Sentry.captureMessage("Stripe webhook: customer ID mismatch for session " + sessionId);
            return false;
        }
        return true;
    }

    private void upgradeByStripeCustomerId(String customerId, String paymentIntentId) {
        if (customerId == null) {
            log.warn("PaymentIntent {} has no customer ID and no userId metadata, cannot upgrade", paymentIntentId);
            return;
        }
        userRepository.findByStripeCustomerIdWithLock(customerId).ifPresentOrElse(user -> {
            if (user.getRole() == Role.PREMIUM_USER || user.getRole() == Role.ADMIN) {
                log.info("User {} already premium/admin, skipping upgrade", user.getId());
                return;
            }
            user.setRole(Role.PREMIUM_USER);
            user.setStripePaymentId(paymentIntentId);
            userRepository.save(user);
            log.info("User {} upgraded to PREMIUM_USER via payment_intent.succeeded", user.getId());
        }, () -> {
            log.error("No user found with stripeCustomerId={} for PaymentIntent {}", customerId, paymentIntentId);
            Sentry.captureMessage("Stripe webhook: no user for customer " + customerId);
        });
    }

    @SuppressWarnings("unchecked")
    private <T extends StripeObject> T deserializeEventObject(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return (T) deserializer.getObject().get();
        }
        log.error("API version mismatch for event {} — refusing unsafe deserialization", event.getId());
        Sentry.captureMessage("Stripe API version mismatch for event " + event.getId());
        throw new RuntimeException("Cannot safely deserialize Stripe event " + event.getId());
    }

    private String getOrCreateStripeCustomer(User user) throws StripeException {
        User lockedUser = userRepository.findByIdWithLock(user.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        if (lockedUser.getStripeCustomerId() != null) {
            return lockedUser.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
            .setEmail(lockedUser.getEmail())
            .putMetadata("userId", lockedUser.getId().toString())
            .build();

        Customer customer = Customer.create(params);
        lockedUser.setStripeCustomerId(customer.getId());
        userRepository.save(lockedUser);
        return customer.getId();
    }
}
