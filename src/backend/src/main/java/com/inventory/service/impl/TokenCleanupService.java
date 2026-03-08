package com.inventory.service.impl;

import com.inventory.repository.StripeWebhookEventRepository;
import com.inventory.repository.VerificationTokenRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final StripeWebhookEventRepository webhookEventRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = verificationTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired verification token(s)", deleted);
        } else {
            log.debug("Token cleanup ran — no expired tokens found");
        }
    }

    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void cleanupOldWebhookEvents() {
        int deleted = webhookEventRepository.deleteByProcessedAtBefore(LocalDateTime.now().minusDays(90));
        if (deleted > 0) {
            log.info("Cleaned up {} old Stripe webhook event(s)", deleted);
        } else {
            log.debug("Webhook event cleanup ran — no old events found");
        }
    }
}
