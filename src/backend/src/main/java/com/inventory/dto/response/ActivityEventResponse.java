package com.inventory.dto.response;

import com.inventory.enums.ActivityEventType;
import com.inventory.model.ActivityEvent;
import com.inventory.model.User;

import java.time.Instant;
import java.util.UUID;

public record ActivityEventResponse(
    UUID id,
    UUID workspaceId,
    UUID actorId,
    String actorName,
    String actorAvatarUrl,
    ActivityEventType action,
    String entityType,
    UUID entityId,
    String entityName,
    Instant occurredAt
) {
    public static ActivityEventResponse fromEntity(ActivityEvent e, User actor) {
        String name = actor != null ? actor.getEmail() : "Deleted user";
        String avatar = actor != null ? actor.getPictureUrl() : null;
        return new ActivityEventResponse(
            e.getId(), e.getWorkspaceId(), e.getActorId(),
            name, avatar, e.getAction(), e.getEntityType(),
            e.getEntityId(), e.getEntityName(), e.getOccurredAt()
        );
    }
}
