package com.inventory.controller;

import com.inventory.enums.ActivityEventType;
import com.inventory.enums.Role;
import com.inventory.enums.WorkspaceRole;
import com.inventory.model.ActivityEvent;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ActivityEventRepository;
import com.inventory.repository.UserRepository;
import com.inventory.repository.WorkspaceMemberRepository;
import com.inventory.repository.WorkspaceRepository;
import com.inventory.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Activity Feed Integration Tests")
class ActivityFeedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ActivityEventRepository activityEventRepository;

    private User memberUser;
    private User outsiderUser;
    private Workspace workspace;
    private ActivityEvent event1;
    private ActivityEvent event2;

    @BeforeEach
    void setUp() {
        // Clean up
        activityEventRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // Create users
        memberUser = new User();
        memberUser.setEmail("member@example.com");
        memberUser.setPassword("password");
        memberUser.setRole(Role.USER);
        memberUser = userRepository.save(memberUser);

        outsiderUser = new User();
        outsiderUser.setEmail("outsider@example.com");
        outsiderUser.setPassword("password");
        outsiderUser.setRole(Role.USER);
        outsiderUser = userRepository.save(outsiderUser);

        // Create workspace
        workspace = new Workspace();
        workspace.setName("Test Workspace");
        workspace.setOwner(memberUser);
        workspace.setDefault(true);
        workspace = workspaceRepository.save(workspace);

        // Add member to workspace
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(memberUser);
        member.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        // Create activity events
        event1 = new ActivityEvent();
        event1.setWorkspaceId(workspace.getId());
        event1.setActorId(memberUser.getId());
        event1.setAction(ActivityEventType.ITEM_CREATED);
        event1.setEntityType("ITEM");
        event1.setEntityId(UUID.randomUUID());
        event1.setEntityName("Test Item 1");
        event1.setOccurredAt(Instant.now().minusSeconds(100));
        event1 = activityEventRepository.save(event1);

        event2 = new ActivityEvent();
        event2.setWorkspaceId(workspace.getId());
        event2.setActorId(memberUser.getId());
        event2.setAction(ActivityEventType.LIST_CREATED);
        event2.setEntityType("LIST");
        event2.setEntityId(UUID.randomUUID());
        event2.setEntityName("Test List 1");
        event2.setOccurredAt(Instant.now().minusSeconds(50));
        event2 = activityEventRepository.save(event2);
    }

    private Cookie jwtCookie(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new Cookie("access_token", token);
    }

    @Nested
    @DisplayName("GET /api/v1/workspaces/{id}/activity")
    class GetActivityFeedTests {

        @Test
        @DisplayName("Workspace member can get activity feed")
        void memberCanGetActivityFeed() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[0].id", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].workspaceId", is(workspace.getId().toString())))
                    .andExpect(jsonPath("$.data.content[0].actorId", is(memberUser.getId().toString())))
                    .andExpect(jsonPath("$.data.content[0].actorName", is(memberUser.getEmail())))
                    .andExpect(jsonPath("$.data.content[0].action", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].entityType", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].entityId", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].entityName", notNullValue()))
                    .andExpect(jsonPath("$.data.content[0].occurredAt", notNullValue()));
        }

        @Test
        @DisplayName("Events are returned in descending order (most recent first)")
        void eventsInDescendingOrder() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].action", is("LIST_CREATED")))
                    .andExpect(jsonPath("$.data.content[1].action", is("ITEM_CREATED")));
        }

        @Test
        @DisplayName("Non-member (outsider) cannot access activity feed")
        void nonMemberCannotAccess() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .cookie(jwtCookie(outsiderUser)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Unauthenticated user cannot access activity feed")
        void unauthenticatedCannotAccess() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("entityType query param filters activity events")
        void entityTypeFilterWorks() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .param("entityType", "ITEM")
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].entityType", is("ITEM")))
                    .andExpect(jsonPath("$.data.content[0].action", is("ITEM_CREATED")));
        }

        @Test
        @DisplayName("entityType filter with no matching events returns empty")
        void entityTypeFilterNoMatches() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .param("entityType", "MEMBER")
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Pagination works with page and size parameters")
        void paginationWorks() throws Exception {
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .param("page", "0")
                            .param("size", "1")
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.page", is(0)))
                    .andExpect(jsonPath("$.data.size", is(1)))
                    .andExpect(jsonPath("$.data.totalElements", is(2)));
        }

        @Test
        @DisplayName("Response includes actorAvatarUrl field (nullable)")
        void responseIncludesActorAvatarUrl() throws Exception {
            // Set a picture URL on the user
            memberUser.setPictureUrl("https://example.com/avatar.jpg");
            userRepository.save(memberUser);

            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace.getId())
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].actorAvatarUrl", is("https://example.com/avatar.jpg")));
        }
    }

    @Nested
    @DisplayName("Activity feed authorization edge cases")
    class AuthorizationEdgeCases {

        @Test
        @DisplayName("Non-existent workspace returns 404")
        void nonExistentWorkspace() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", nonExistentId)
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Member with different workspace role can still access activity")
        void memberWithViewerRoleCanAccess() throws Exception {
            // Create another workspace
            Workspace workspace2 = new Workspace();
            workspace2.setName("Workspace 2");
            workspace2.setOwner(memberUser);
            workspace2.setDefault(false);
            workspace2 = workspaceRepository.save(workspace2);

            // Add member with VIEWER role
            WorkspaceMember viewerMember = new WorkspaceMember();
            viewerMember.setWorkspace(workspace2);
            viewerMember.setUser(memberUser);
            viewerMember.setRole(WorkspaceRole.VIEWER);
            workspaceMemberRepository.save(viewerMember);

            // Create activity event in workspace2
            ActivityEvent event = new ActivityEvent();
            event.setWorkspaceId(workspace2.getId());
            event.setActorId(memberUser.getId());
            event.setAction(ActivityEventType.ITEM_UPDATED);
            event.setEntityType("ITEM");
            event.setEntityId(UUID.randomUUID());
            event.setEntityName("Test Item");
            event.setOccurredAt(Instant.now());
            activityEventRepository.save(event);

            mockMvc.perform(get("/api/v1/workspaces/{id}/activity", workspace2.getId())
                            .cookie(jwtCookie(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)));
        }
    }
}
