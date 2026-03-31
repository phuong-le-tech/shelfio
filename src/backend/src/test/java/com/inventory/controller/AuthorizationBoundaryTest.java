package com.inventory.controller;

import com.inventory.enums.Role;
import com.inventory.enums.WorkspaceRole;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
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

import com.inventory.model.ItemList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authorization Boundary Tests")
class AuthorizationBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemListRepository itemListRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    private User userA;
    private User userB;
    private User adminUser;
    private ItemList userAList;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        itemListRepository.deleteAll();
        userRepository.deleteAll();

        userA = new User();
        userA.setEmail("usera@example.com");
        userA.setPassword("password");
        userA.setRole(Role.USER);
        userA = userRepository.save(userA);

        userB = new User();
        userB.setEmail("userb@example.com");
        userB.setPassword("password");
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);

        adminUser = new User();
        adminUser.setEmail("testadmin@boundary-test.com");
        adminUser.setPassword("password");
        adminUser.setRole(Role.ADMIN);
        adminUser = userRepository.save(adminUser);

        Workspace workspaceA = new Workspace();
        workspaceA.setName("User A Workspace");
        workspaceA.setOwner(userA);
        workspaceA.setDefault(true);
        workspaceA = workspaceRepository.save(workspaceA);

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(workspaceA);
        memberA.setUser(userA);
        memberA.setRole(WorkspaceRole.OWNER);
        workspaceMemberRepository.save(memberA);

        userAList = new ItemList();
        userAList.setName("User A's List");
        userAList.setCategory("Electronics");
        userAList.setUser(userA);
        userAList.setWorkspace(workspaceA);
        userAList = itemListRepository.save(userAList);
    }

    private Cookie jwtCookie(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new Cookie("access_token", token);
    }

    @Nested
    @DisplayName("Unauthenticated requests")
    class UnauthenticatedTests {

        @Test
        @DisplayName("GET /api/v1/items without token should be rejected")
        void items_noToken_rejected() throws Exception {
            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/v1/lists without token should be rejected")
        void lists_noToken_rejected() throws Exception {
            mockMvc.perform(get("/api/v1/lists"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/v1/admin/users without token should be rejected")
        void admin_noToken_rejected() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/v1/lists/{id}/duplicate without token should be rejected")
        void duplicate_noToken_rejected() throws Exception {
            mockMvc.perform(post("/api/v1/lists/{id}/duplicate", userAList.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Regular user vs admin endpoints")
    class UserVsAdminTests {

        @Test
        @DisplayName("Regular user accessing GET /api/v1/admin/users should return 403")
        void regularUser_adminEndpoint_returns403() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                            .cookie(jwtCookie(userA)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Admin accessing GET /api/v1/admin/users should return 200")
        void admin_adminEndpoint_returns200() throws Exception {
            mockMvc.perform(get("/api/v1/admin/users")
                            .cookie(jwtCookie(adminUser)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Cross-user access control")
    class CrossUserAccessTests {

        @Test
        @DisplayName("User B cannot access User A's list")
        void userB_cannotAccessUserAList() throws Exception {
            mockMvc.perform(get("/api/v1/lists/{id}", userAList.getId())
                            .cookie(jwtCookie(userB)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User A can access their own list")
        void userA_canAccessOwnList() throws Exception {
            mockMvc.perform(get("/api/v1/lists/{id}", userAList.getId())
                            .cookie(jwtCookie(userA)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("User B cannot delete User A's list")
        void userB_cannotDeleteUserAList() throws Exception {
            mockMvc.perform(delete("/api/v1/lists/{id}", userAList.getId())
                            .cookie(jwtCookie(userB)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Admin can access any user's list")
        void admin_canAccessAnyList() throws Exception {
            mockMvc.perform(get("/api/v1/lists/{id}", userAList.getId())
                            .cookie(jwtCookie(adminUser)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Invalid JWT")
    class InvalidJwtTests {

        @Test
        @DisplayName("Expired/invalid JWT should be rejected")
        void expiredToken_rejected() throws Exception {
            Cookie cookie = new Cookie("access_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiZW1haWwiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZSI6IlVTRVIiLCJleHAiOjF9.invalid");
            mockMvc.perform(get("/api/v1/items")
                            .cookie(cookie))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Tampered JWT should be rejected")
        void tamperedToken_rejected() throws Exception {
            String validToken = jwtService.generateToken(userA.getId(), userA.getEmail(), "USER");
            String tampered = validToken.substring(0, validToken.length() - 2) + "XX";
            Cookie cookie = new Cookie("access_token", tampered);
            mockMvc.perform(get("/api/v1/items")
                            .cookie(cookie))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Random string as JWT should be rejected")
        void randomString_rejected() throws Exception {
            Cookie cookie = new Cookie("access_token", "not-a-jwt-at-all");
            mockMvc.perform(get("/api/v1/items")
                            .cookie(cookie))
                    .andExpect(status().isForbidden());
        }
    }
}
