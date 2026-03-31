package com.inventory.service;

import com.inventory.dto.response.ActivityEventResponse;
import com.inventory.enums.ActivityEventType;
import com.inventory.model.ActivityEvent;
import com.inventory.model.User;
import com.inventory.repository.ActivityEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.service.impl.ActivityServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityServiceImpl Tests")
class ActivityServiceImplTest {

    @Mock
    private ActivityEventRepository activityEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Nested
    @DisplayName("record()")
    class RecordTests {

        @Test
        @DisplayName("saves event with correct fields")
        void savesEventWithCorrectFields() {
            UUID actorId = UUID.randomUUID();
            UUID workspaceId = UUID.randomUUID();
            UUID entityId = UUID.randomUUID();
            String entityName = "My Item";

            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(actorId));
            when(activityEventRepository.save(any(ActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            activityService.record(workspaceId, ActivityEventType.ITEM_CREATED, "item", entityId, entityName);

            ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
            verify(activityEventRepository).save(captor.capture());
            ActivityEvent saved = captor.getValue();

            assertThat(saved.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(saved.getActorId()).isEqualTo(actorId);
            assertThat(saved.getAction()).isEqualTo(ActivityEventType.ITEM_CREATED);
            assertThat(saved.getEntityType()).isEqualTo("item");
            assertThat(saved.getEntityId()).isEqualTo(entityId);
            assertThat(saved.getEntityName()).isEqualTo(entityName);
        }

        @Test
        @DisplayName("sets actorId to null when no authenticated user")
        void setsActorIdToNullWhenNoAuthenticatedUser() {
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());
            when(activityEventRepository.save(any(ActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            activityService.record(UUID.randomUUID(), ActivityEventType.LIST_CREATED, "list",
                    UUID.randomUUID(), "My List");

            ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
            verify(activityEventRepository).save(captor.capture());

            assertThat(captor.getValue().getActorId()).isNull();
        }

        @Test
        @DisplayName("truncates entityName longer than 255 chars")
        void truncatesEntityNameLongerThan255Chars() {
            String longName = "A".repeat(300);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());
            when(activityEventRepository.save(any(ActivityEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            activityService.record(UUID.randomUUID(), ActivityEventType.ITEM_UPDATED, "item",
                    UUID.randomUUID(), longName);

            ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
            verify(activityEventRepository).save(captor.capture());

            assertThat(captor.getValue().getEntityName()).hasSize(255);
        }

        @Test
        @DisplayName("does not propagate exception when save fails")
        void doesNotPropagateExceptionWhenSaveFails() {
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());
            when(activityEventRepository.save(any(ActivityEvent.class)))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatCode(() ->
                    activityService.record(UUID.randomUUID(), ActivityEventType.ITEM_DELETED,
                            "item", UUID.randomUUID(), "Item"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getActivity()")
    class GetActivityTests {

        @Test
        @DisplayName("returns page mapped to ActivityEventResponse with correct actorName and action")
        void returnsMappedPage() {
            UUID workspaceId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            ActivityEvent event = new ActivityEvent();
            event.setWorkspaceId(workspaceId);
            event.setActorId(actorId);
            event.setAction(ActivityEventType.ITEM_CREATED);
            event.setEntityType("item");
            event.setEntityId(UUID.randomUUID());
            event.setEntityName("Test Item");

            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityEvent> eventPage = new PageImpl<>(List.of(event), pageable, 1);

            User actor = new User();
            actor.setId(actorId);
            actor.setEmail("actor@example.com");

            when(activityEventRepository.findFiltered(eq(workspaceId), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                    .thenReturn(eventPage);
            when(userRepository.findAllById(anyCollection())).thenReturn(List.of(actor));

            Page<ActivityEventResponse> result = activityService.getActivity(
                    workspaceId, null, null, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            ActivityEventResponse response = result.getContent().get(0);
            assertThat(response.actorName()).isEqualTo("actor@example.com");
            assertThat(response.action()).isEqualTo(ActivityEventType.ITEM_CREATED);
        }

        @Test
        @DisplayName("handles deleted actor (null actorId) — actorName is 'Deleted user'")
        void handlesDeletedActor() {
            UUID workspaceId = UUID.randomUUID();

            ActivityEvent event = new ActivityEvent();
            event.setWorkspaceId(workspaceId);
            event.setActorId(null);
            event.setAction(ActivityEventType.ITEM_DELETED);
            event.setEntityType("item");
            event.setEntityId(UUID.randomUUID());
            event.setEntityName("Gone Item");

            Pageable pageable = PageRequest.of(0, 20);
            Page<ActivityEvent> eventPage = new PageImpl<>(List.of(event), pageable, 1);

            when(activityEventRepository.findFiltered(any(), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(eventPage);
            when(userRepository.findAllById(anyCollection())).thenReturn(List.of());

            Page<ActivityEventResponse> result = activityService.getActivity(
                    workspaceId, null, null, null, null, pageable);

            assertThat(result.getContent().get(0).actorName()).isEqualTo("Deleted user");
        }
    }

    @Nested
    @DisplayName("purgeOldEvents()")
    class PurgeOldEventsTests {

        @Test
        @DisplayName("calls deleteOlderThan with ~90 days ago cutoff")
        void callsDeleteOlderThanWith90DaysCutoff() {
            activityService.purgeOldEvents();

            ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
            verify(activityEventRepository).deleteOlderThan(cutoffCaptor.capture());

            Instant expected = Instant.now().minus(90, ChronoUnit.DAYS);
            assertThat(cutoffCaptor.getValue()).isCloseTo(expected, within(1, ChronoUnit.SECONDS));
        }
    }
}
