package com.inventory.repository;

import com.inventory.model.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, UUID> {
    int deleteByProcessedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query(value = "INSERT INTO stripe_webhook_events (id, stripe_event_id, event_type, processed_at) " +
                   "VALUES (:id, :stripeEventId, :eventType, NOW()) ON CONFLICT (stripe_event_id) DO NOTHING",
           nativeQuery = true)
    int insertIfNotExists(@Param("id") UUID id, @Param("stripeEventId") String stripeEventId,
                          @Param("eventType") String eventType);
}
