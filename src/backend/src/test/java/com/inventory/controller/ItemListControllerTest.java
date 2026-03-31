package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.TestSecurityConfig;
import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.exception.ExportLimitExceededException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ListLimitExceededException;
import com.inventory.exception.WorkspaceAccessDeniedException;
import com.inventory.model.ItemList;
import com.inventory.security.CustomUserDetails;
import com.inventory.service.IItemListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ItemListController Tests")
class ItemListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IItemListService itemListService;

    private ItemList testList;
    private UUID testListId;
    private UUID userId;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        testListId = UUID.randomUUID();
        userId = UUID.randomUUID();
        userDetails = new CustomUserDetails(userId, "test@test.com", "USER");

        testList = new ItemList();
        testList.setId(testListId);
        testList.setName("Test List");
        testList.setDescription("A test list");
        testList.setCategory("Electronics");
        testList.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
        testList.setUpdatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
    }

    @Nested
    @DisplayName("GET /api/v1/lists")
    class GetAllListsTests {

        @Test
        @DisplayName("should return paginated lists with default parameters")
        void getAllLists_defaultParams_returnsPageResponse() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(testList), expectedPageable, 1));

            mockMvc.perform(get("/api/v1/lists")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(testListId.toString()))
                    .andExpect(jsonPath("$.data.content[0].name").value("Test List"))
                    .andExpect(jsonPath("$.data.content[0].description").value("A test list"))
                    .andExpect(jsonPath("$.data.content[0].category").value("Electronics"))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("should support custom pagination and sorting parameters")
        void getAllLists_customParams_passesCorrectPageable() throws Exception {
            Pageable expectedPageable = PageRequest.of(1, 5, Sort.by("name").ascending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sortBy", "name")
                            .param("sortDir", "asc")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        @DisplayName("should fall back to createdAt when sort field is not allowed")
        void getAllLists_invalidSortField_fallsBackToCreatedAt() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("sortBy", "invalidField")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(itemListService).getAllLists(expectedPageable, null, null);
        }

        @Test
        @DisplayName("should clamp size to minimum of 1 when size is 0 or negative")
        void getAllLists_sizeTooSmall_clampsToOne() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 1, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("size", "0")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(itemListService).getAllLists(expectedPageable, null, null);
        }

        @Test
        @DisplayName("should clamp size to maximum of 100 when size exceeds limit")
        void getAllLists_sizeTooLarge_clampsTo100() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 100, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("size", "200")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(itemListService).getAllLists(expectedPageable, null, null);
        }

        @Test
        @DisplayName("should return empty page when no lists exist")
        void getAllLists_noLists_returnsEmptyPage() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("should support sorting by updatedAt ascending")
        void getAllLists_sortByUpdatedAtAsc_passesCorrectSort() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("updatedAt").ascending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("sortBy", "updatedAt")
                            .param("sortDir", "asc")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(itemListService).getAllLists(expectedPageable, null, null);
        }

        @Test
        @DisplayName("should forward search parameter to service")
        void getAllLists_withSearch_forwardsToService() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            when(itemListService.getAllLists(expectedPageable, null, "electronics"))
                    .thenReturn(new PageImpl<>(List.of(testList), expectedPageable, 1));

            mockMvc.perform(get("/api/v1/lists")
                            .param("search", "electronics")
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].name").value("Test List"));

            verify(itemListService).getAllLists(expectedPageable, null, "electronics");
        }

        @Test
        @DisplayName("should return 400 when search exceeds 255 characters")
        void getAllLists_searchTooLong_returns400() throws Exception {
            String longSearch = "a".repeat(256);

            mockMvc.perform(get("/api/v1/lists")
                            .param("search", longSearch)
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should support sorting by category")
        void getAllLists_sortByCategory_passesCorrectSort() throws Exception {
            Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("category").descending());
            when(itemListService.getAllLists(expectedPageable, null, null))
                    .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

            mockMvc.perform(get("/api/v1/lists")
                            .param("sortBy", "category")
                            .with(user(userDetails)))
                    .andExpect(status().isOk());

            verify(itemListService).getAllLists(expectedPageable, null, null);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lists/{id}")
    class GetListByIdTests {

        @Test
        @DisplayName("should return list when it exists")
        void getListById_existingId_returnsList() throws Exception {
            when(itemListService.getListById(testListId)).thenReturn(testList);

            mockMvc.perform(get("/api/v1/lists/{id}", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testListId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Test List"))
                    .andExpect(jsonPath("$.data.description").value("A test list"))
                    .andExpect(jsonPath("$.data.category").value("Electronics"));
        }

        @Test
        @DisplayName("should return 404 when list not found")
        void getListById_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            when(itemListService.getListById(nonExistingId))
                    .thenThrow(new ItemListNotFoundException(nonExistingId));

            mockMvc.perform(get("/api/v1/lists/{id}", nonExistingId)
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404))
                    .andExpect(jsonPath("$.error.message").value("Item list not found with id: " + nonExistingId));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/lists")
    class CreateListTests {

        @Test
        @DisplayName("should create list with valid request")
        void createList_validRequest_returns201() throws Exception {
            ItemListRequest request = new ItemListRequest("New List", "Description", "Category", null, null);
            when(itemListService.createList(any(ItemListRequest.class))).thenReturn(testList);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(testListId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Test List"));
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void createList_blankName_returns400() throws Exception {
            ItemListRequest request = new ItemListRequest("", "Description", "Category", null, null);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when name is null")
        void createList_nullName_returns400() throws Exception {
            ItemListRequest request = new ItemListRequest(null, "Description", "Category", null, null);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }

        @Test
        @DisplayName("should return 400 when name exceeds 100 characters")
        void createList_nameTooLong_returns400() throws Exception {
            String longName = "a".repeat(101);
            ItemListRequest request = new ItemListRequest(longName, "Description", "Category", null, null);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400))
                    .andExpect(jsonPath("$.error.message").value("name: Name must be at most 100 characters"));
        }

        @Test
        @DisplayName("should return 400 when description exceeds 500 characters")
        void createList_descriptionTooLong_returns400() throws Exception {
            String longDescription = "a".repeat(501);
            ItemListRequest request = new ItemListRequest("Valid Name", longDescription, "Category", null, null);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400))
                    .andExpect(jsonPath("$.error.message").value("description: Description must be at most 500 characters"));
        }

        @Test
        @DisplayName("should return 400 when category exceeds 100 characters")
        void createList_categoryTooLong_returns400() throws Exception {
            String longCategory = "a".repeat(101);
            ItemListRequest request = new ItemListRequest("Valid Name", "Description", longCategory, null, null);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400))
                    .andExpect(jsonPath("$.error.message").value("category: Category must be at most 100 characters"));
        }

        @Test
        @DisplayName("should create list with only required fields")
        void createList_onlyRequiredFields_returns201() throws Exception {
            ItemListRequest request = new ItemListRequest("Minimal List", null, null, null, null);
            when(itemListService.createList(any(ItemListRequest.class))).thenReturn(testList);

            mockMvc.perform(post("/api/v1/lists")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/lists/{id}")
    class UpdateListTests {

        @Test
        @DisplayName("should update existing list")
        void updateList_existingId_returnsUpdatedList() throws Exception {
            ItemListRequest request = new ItemListRequest("Updated List", "Updated Desc", "Updated Cat", null, null);

            ItemList updatedList = new ItemList();
            updatedList.setId(testListId);
            updatedList.setName("Updated List");
            updatedList.setDescription("Updated Desc");
            updatedList.setCategory("Updated Cat");
            updatedList.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
            updatedList.setUpdatedAt(LocalDateTime.of(2025, 1, 16, 12, 0));

            when(itemListService.updateList(eq(testListId), any(ItemListRequest.class)))
                    .thenReturn(updatedList);

            mockMvc.perform(patch("/api/v1/lists/{id}", testListId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testListId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Updated List"))
                    .andExpect(jsonPath("$.data.description").value("Updated Desc"))
                    .andExpect(jsonPath("$.data.category").value("Updated Cat"));
        }

        @Test
        @DisplayName("should return 404 when updating non-existing list")
        void updateList_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            ItemListRequest request = new ItemListRequest("Updated List", "Updated Desc", "Updated Cat", null, null);

            when(itemListService.updateList(eq(nonExistingId), any(ItemListRequest.class)))
                    .thenThrow(new ItemListNotFoundException(nonExistingId));

            mockMvc.perform(patch("/api/v1/lists/{id}", nonExistingId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404))
                    .andExpect(jsonPath("$.error.message").value("Item list not found with id: " + nonExistingId));
        }

        @Test
        @DisplayName("should return 400 when updating with blank name")
        void updateList_blankName_returns400() throws Exception {
            ItemListRequest request = new ItemListRequest("", "Description", "Category", null, null);

            mockMvc.perform(patch("/api/v1/lists/{id}", testListId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/lists/{id}")
    class DeleteListTests {

        @Test
        @DisplayName("should delete existing list and return 204")
        void deleteList_existingId_returns204() throws Exception {
            doNothing().when(itemListService).deleteList(testListId);

            mockMvc.perform(delete("/api/v1/lists/{id}", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isNoContent());

            verify(itemListService).deleteList(testListId);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existing list")
        void deleteList_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            doThrow(new ItemListNotFoundException(nonExistingId))
                    .when(itemListService).deleteList(nonExistingId);

            mockMvc.perform(delete("/api/v1/lists/{id}", nonExistingId)
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404))
                    .andExpect(jsonPath("$.error.message").value("Item list not found with id: " + nonExistingId));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/lists/{id}/duplicate")
    @SuppressWarnings("null")
    class DuplicateListTests {

        @Test
        @DisplayName("should return 201 with duplicated list")
        void duplicateList_success_returns201() throws Exception {
            ItemList duplicated = new ItemList();
            duplicated.setId(UUID.randomUUID());
            duplicated.setName("Test List (Copie)");
            duplicated.setDescription("A test list");
            duplicated.setCategory("Electronics");
            duplicated.setCreatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));
            duplicated.setUpdatedAt(LocalDateTime.of(2025, 1, 15, 10, 0));

            when(itemListService.duplicateList(testListId)).thenReturn(duplicated);

            mockMvc.perform(post("/api/v1/lists/{id}/duplicate", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Test List (Copie)"))
                    .andExpect(jsonPath("$.data.category").value("Electronics"));

            verify(itemListService).duplicateList(testListId);
        }

        @Test
        @DisplayName("should return 404 when source list not found")
        void duplicateList_notFound_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            when(itemListService.duplicateList(nonExistingId))
                    .thenThrow(new ItemListNotFoundException(nonExistingId));

            mockMvc.perform(post("/api/v1/lists/{id}/duplicate", nonExistingId)
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }

        @Test
        @DisplayName("should return 403 when VIEWER tries to duplicate")
        void duplicateList_viewer_returns403() throws Exception {
            when(itemListService.duplicateList(testListId))
                    .thenThrow(new WorkspaceAccessDeniedException("Viewers cannot duplicate lists"));

            mockMvc.perform(post("/api/v1/lists/{id}/duplicate", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when free-tier limit is reached")
        void duplicateList_limitReached_returns403() throws Exception {
            when(itemListService.duplicateList(testListId))
                    .thenThrow(new ListLimitExceededException("Free plan limited to 5 lists."));

            mockMvc.perform(post("/api/v1/lists/{id}/duplicate", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value(403));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/lists/{id}/export")
    class ExportListTests {

        @Test
        @DisplayName("should return CSV with correct headers")
        void exportList_existingId_returnsCsvWithHeaders() throws Exception {
            byte[] csvContent = "\uFEFFName,Status,Stock\r\n".getBytes(StandardCharsets.UTF_8);
            CsvExportResult csvResult = new CsvExportResult(csvContent, "Test_List_2026-03-14.csv");
            when(itemListService.exportListAsCsv(testListId)).thenReturn(csvResult);

            mockMvc.perform(get("/api/v1/lists/{id}/export", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("attachment")))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("Test_List_2026-03-14.csv")))
                    .andExpect(header().longValue("Content-Length", csvContent.length))
                    .andExpect(header().string("Cache-Control", "no-store, no-cache, must-revalidate"))
                    .andExpect(header().string("Pragma", "no-cache"))
                    .andExpect(content().bytes(csvContent));
        }

        @Test
        @DisplayName("should return 400 when export exceeds item limit")
        void exportList_exceedsLimit_returns400() throws Exception {
            when(itemListService.exportListAsCsv(testListId))
                    .thenThrow(new ExportLimitExceededException("Cannot export more than 10000 items. Please reduce the list size."));

            mockMvc.perform(get("/api/v1/lists/{id}/export", testListId)
                            .with(user(userDetails)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value(400))
                    .andExpect(jsonPath("$.error.message").value("Cannot export more than 10000 items. Please reduce the list size."));
        }

        @Test
        @DisplayName("should return 404 when list not found")
        void exportList_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            when(itemListService.exportListAsCsv(nonExistingId))
                    .thenThrow(new ItemListNotFoundException(nonExistingId));

            mockMvc.perform(get("/api/v1/lists/{id}/export", nonExistingId)
                            .with(user(userDetails)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value(404));
        }
    }
}
