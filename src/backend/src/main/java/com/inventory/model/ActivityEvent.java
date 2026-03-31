package com.inventory.model;

import com.inventory.enums.ActivityEventType;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
@Data
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityEventType action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
