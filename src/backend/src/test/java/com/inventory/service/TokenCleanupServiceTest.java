package com.inventory.service;

import com.inventory.repository.StripeWebhookEventRepository;
import com.inventory.repository.VerificationTokenRepository;
import com.inventory.service.impl.TokenCleanupService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenCleanupService Tests")
class TokenCleanupServiceTest {

    @Mock
    private VerificationTokenRepository verificationTokenRepository;

    @Mock
    private StripeWebhookEventRepository webhookEventRepository;

    @InjectMocks
    private TokenCleanupService tokenCleanupService;

    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokensTests {

        @Test
        @DisplayName("should delete expired tokens when found")
        void cleanupExpiredTokens_withExpiredTokens_deletesAndLogs() {
            when(verificationTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
                    .thenReturn(3);

            tokenCleanupService.cleanupExpiredTokens();

            verify(verificationTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should log debug when no expired tokens found")
        void cleanupExpiredTokens_noExpiredTokens_logsDebug() {
            when(verificationTokenRepository.deleteByExpiresAtBefore(any(LocalDateTime.class)))
                    .thenReturn(0);

            tokenCleanupService.cleanupExpiredTokens();

            verify(verificationTokenRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("cleanupOldWebhookEvents")
    class CleanupOldWebhookEventsTests {

        @Test
        @DisplayName("should delete old webhook events when found")
        void cleanupOldWebhookEvents_withOldEvents_deletesAndLogs() {
            when(webhookEventRepository.deleteByProcessedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(5);

            tokenCleanupService.cleanupOldWebhookEvents();

            verify(webhookEventRepository).deleteByProcessedAtBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("should log debug when no old webhook events found")
        void cleanupOldWebhookEvents_noOldEvents_logsDebug() {
            when(webhookEventRepository.deleteByProcessedAtBefore(any(LocalDateTime.class)))
                    .thenReturn(0);

            tokenCleanupService.cleanupOldWebhookEvents();

            verify(webhookEventRepository).deleteByProcessedAtBefore(any(LocalDateTime.class));
        }
    }
}
