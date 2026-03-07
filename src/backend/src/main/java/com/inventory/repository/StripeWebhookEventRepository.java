package com.inventory.repository;

import com.inventory.model.StripeWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent, UUID> {
    int deleteByProcessedAtBefore(LocalDateTime cutoff);
}
