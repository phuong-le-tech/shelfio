package com.inventory.repository;

import com.inventory.model.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID>,
        JpaSpecificationExecutor<ActivityEvent> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ActivityEvent e WHERE e.occurredAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") Instant cutoff);
}
