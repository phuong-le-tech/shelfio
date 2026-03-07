package com.inventory.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Slf4j
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    private final Environment environment;

    public StripeConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            if (secretKey == null || secretKey.isBlank()) {
                throw new IllegalStateException("STRIPE_SECRET_KEY must be set in production");
            }
            if (webhookSecret == null || webhookSecret.isBlank()) {
                throw new IllegalStateException("STRIPE_WEBHOOK_SECRET must be set in production");
            }
            Stripe.apiKey = secretKey;
            log.info("Stripe API configured for production");
        } else {
            if (secretKey != null && !secretKey.isBlank()) {
                Stripe.apiKey = secretKey;
                log.info("Stripe API key configured for development");
                if (webhookSecret == null || webhookSecret.isBlank()) {
                    log.warn("STRIPE_WEBHOOK_SECRET not set — webhook signature verification will fail. "
                        + "Set it via env var or use Stripe CLI: stripe listen --forward-to localhost:8080/api/v1/stripe/webhook");
                }
            } else {
                log.warn("Stripe API key not configured — payment features disabled");
            }
        }
    }
}
