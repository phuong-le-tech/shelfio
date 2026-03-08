package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.TestSecurityConfig;
import com.inventory.dto.request.CreateUserRequest;
import com.inventory.dto.request.UpdateRoleRequest;
import com.inventory.enums.Role;
import com.inventory.exception.UserAlreadyExistsException;
import com.inventory.exception.UserNotFoundException;
import com.inventory.model.User;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUserService userService;

    private CustomUserDetails adminUser;
    private User testUser;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        adminUser = new CustomUserDetails(UUID.randomUUID(), "admin@test.com", "ADMIN");

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("user@test.com");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/v1/admin/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("should return paginated list of users")
        void getAllUsers_returnsOkWithPageResponse() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testUser)));

            mockMvc.perform(get("/api/v1/admin/users")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.content[0].role").value("USER"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("should support pagination and sorting parameters")
        void getAllUsers_withPaginationParams_returnsPagedResults() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testUser)));

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("page", "0")
                            .param("size", "5")
                            .param("sortBy", "email")
                            .param("sortDir", "asc")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());
        }

        @Test
        @DisplayName("should fall back to createdAt when sort field is invalid")
        void getAllUsers_invalidSortField_fallsBackToCreatedAt() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(testUser)));

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("sortBy", "invalidField")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(userService).getAllUsers(any(Pageable.class));
        }

        @Test
        @DisplayName("should clamp size to minimum of 1")
        void getAllUsers_sizeZero_clampedToOne() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("size", "0")
                            .with(user(adminUser)))
                    .andExpect(status().isOk());

            verify(userService).getAllUsers(any(Pageable.class));
        }

        @Test
        @DisplayName("should clamp size to maximum of 100")
        void getAllUsers_sizeExceedsMax_clampedTo100() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/admin/users")
                            .param("size", "200")
                            .with(user(adminUser)))
                    .andExpect(status().isOk());

            verify(userService).getAllUsers(any(Pageable.class));
        }

        @Test
        @DisplayName("should return empty page when no users exist")
        void getAllUsers_noUsers_returnsEmptyPage() throws Exception {
            when(userService.getAllUsers(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/admin/users")
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/users")
    class CreateUserTests {

        @Test
        @DisplayName("should create user with valid request")
        void createUser_validRequest_returns201() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "newuser@test.com", "TestPassword1!", Role.USER);

            when(userService.createUser(any(CreateUserRequest.class))).thenReturn(testUser);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        }

        @Test
        @DisplayName("should return 409 when user already exists")
        void createUser_duplicateEmail_returns409() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "existing@test.com", "TestPassword1!", Role.USER);

            when(userService.createUser(any(CreateUserRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("existing@test.com"));

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value(409));
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void createUser_blankEmail_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "", "TestPassword1!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when email format is invalid")
        void createUser_invalidEmail_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "not-an-email", "TestPassword1!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password is blank")
        void createUser_blankPassword_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password is too short")
        void createUser_shortPassword_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "Short1!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password lacks uppercase letter")
        void createUser_noUppercase_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "testpassword1!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password lacks lowercase letter")
        void createUser_noLowercase_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "TESTPASSWORD1!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password lacks digit")
        void createUser_noDigit_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "TestPassword!!", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when password lacks special character")
        void createUser_noSpecialChar_returns400() throws Exception {
            CreateUserRequest request = new CreateUserRequest(
                    "user@test.com", "TestPassword12", Role.USER);

            mockMvc.perform(post("/api/v1/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/users/{id}")
    class DeleteUserTests {

        @Test
        @DisplayName("should delete existing user")
        void deleteUser_existingId_returns204() throws Exception {
            doNothing().when(userService).deleteUser(testUserId);

            mockMvc.perform(delete("/api/v1/admin/users/{id}", testUserId)
                            .with(user(adminUser)))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(testUserId);
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void deleteUser_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            doThrow(new UserNotFoundException(nonExistingId))
                    .when(userService).deleteUser(nonExistingId);

            mockMvc.perform(delete("/api/v1/admin/users/{id}", nonExistingId)
                            .with(user(adminUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/users/{id}/role")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("should update user role")
        void updateUserRole_validRequest_returnsUpdatedUser() throws Exception {
            User updatedUser = new User();
            updatedUser.setId(testUserId);
            updatedUser.setEmail("user@test.com");
            updatedUser.setRole(Role.ADMIN);
            updatedUser.setEnabled(true);
            updatedUser.setCreatedAt(LocalDateTime.now());

            UpdateRoleRequest request = new UpdateRoleRequest(Role.ADMIN);

            when(userService.updateUserRole(eq(testUserId), eq(Role.ADMIN)))
                    .thenReturn(updatedUser);

            mockMvc.perform(patch("/api/v1/admin/users/{id}/role", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value("user@test.com"))
                    .andExpect(jsonPath("$.data.role").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void updateUserRole_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            UpdateRoleRequest request = new UpdateRoleRequest(Role.ADMIN);

            when(userService.updateUserRole(eq(nonExistingId), eq(Role.ADMIN)))
                    .thenThrow(new UserNotFoundException(nonExistingId));

            mockMvc.perform(patch("/api/v1/admin/users/{id}/role", nonExistingId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(adminUser)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }

        @Test
        @DisplayName("should return 400 when role is null")
        void updateUserRole_nullRole_returns400() throws Exception {
            String requestBody = "{}";

            mockMvc.perform(patch("/api/v1/admin/users/{id}/role", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody)
                            .with(user(adminUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }
    }
}
