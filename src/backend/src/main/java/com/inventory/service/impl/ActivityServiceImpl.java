package com.inventory.service.impl;

import com.inventory.dto.response.ActivityEventResponse;
import com.inventory.enums.ActivityEventType;
import com.inventory.model.ActivityEvent;
import com.inventory.model.User;
import com.inventory.repository.ActivityEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.service.IActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements IActivityService {

    private final ActivityEventRepository activityEventRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID workspaceId, ActivityEventType action,
                       String entityType, UUID entityId, String entityName) {
        try {
            UUID actorId = securityUtils.getCurrentUserId().orElse(null);

            ActivityEvent event = new ActivityEvent();
            event.setWorkspaceId(workspaceId);
            event.setActorId(actorId);
            event.setAction(action);
            event.setEntityType(entityType);
            event.setEntityId(entityId);
            event.setEntityName(entityName != null && entityName.length() > 255
                    ? entityName.substring(0, 255) : entityName);

            activityEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record activity event: workspace={} action={} entityType={} entityId={}",
                    workspaceId, action, entityType, entityId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getActivity(UUID workspaceId, UUID actorId,
                                                   String entityType, Instant from,
                                                   Instant to, Pageable pageable) {
        Page<ActivityEvent> page = activityEventRepository.findFiltered(
                workspaceId, actorId, entityType, from, to, pageable);

        Set<UUID> actorIds = page.getContent().stream()
                .map(ActivityEvent::getActorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<UUID, User> actorMap = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return page.map(e -> ActivityEventResponse.fromEntity(e, actorMap.get(e.getActorId())));
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldEvents() {
        activityEventRepository.deleteOlderThan(Instant.now().minus(90, ChronoUnit.DAYS));
    }
}
