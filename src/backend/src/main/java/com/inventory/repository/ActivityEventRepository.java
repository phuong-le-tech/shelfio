package com.inventory.repository;

import com.inventory.model.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {

    @Query(value = """
        SELECT e FROM ActivityEvent e
        WHERE e.workspaceId = :workspaceId
          AND (:actorId IS NULL OR e.actorId = :actorId)
          AND (:entityType IS NULL OR e.entityType = :entityType)
          AND (:from IS NULL OR e.occurredAt >= :from)
          AND (:to IS NULL OR e.occurredAt <= :to)
        ORDER BY e.occurredAt DESC
        """,
        countQuery = """
        SELECT COUNT(e) FROM ActivityEvent e
        WHERE e.workspaceId = :workspaceId
          AND (:actorId IS NULL OR e.actorId = :actorId)
          AND (:entityType IS NULL OR e.entityType = :entityType)
          AND (:from IS NULL OR e.occurredAt >= :from)
          AND (:to IS NULL OR e.occurredAt <= :to)
        """)
    Page<ActivityEvent> findFiltered(
            @Param("workspaceId") UUID workspaceId,
            @Param("actorId") UUID actorId,
            @Param("entityType") String entityType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActivityEvent e WHERE e.occurredAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") Instant cutoff);
}
