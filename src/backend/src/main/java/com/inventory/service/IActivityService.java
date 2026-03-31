package com.inventory.service;

import com.inventory.dto.response.ActivityEventResponse;
import com.inventory.enums.ActivityEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface IActivityService {

    void record(UUID workspaceId, ActivityEventType action,
                String entityType, UUID entityId, String entityName);

    Page<ActivityEventResponse> getActivity(UUID workspaceId,
                                            UUID actorId,
                                            String entityType,
                                            Instant from,
                                            Instant to,
                                            Pageable pageable);
}
