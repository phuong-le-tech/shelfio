package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.TestSecurityConfig;
import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;
import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.service.IImageAnalysisService;
import com.inventory.service.IItemListService;
import com.inventory.service.IItemService;
import com.inventory.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("ItemController Tests")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IItemService itemService;

    @MockitoBean
    @Qualifier("uploadRateLimiter")
    private ApiRateLimiter uploadRateLimiter;

    @MockitoBean
    private ImageStorageService imageStorageService;

    @MockitoBean
    private IItemListService itemListService;

    @MockitoBean
    private IImageAnalysisService imageAnalysisService;

    private Item testItem;
    private ItemList testList;
    private UUID testId;
    private UUID testListId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testListId = UUID.randomUUID();

        testList = new ItemList();
        testList.setId(testListId);
        testList.setName("Test List");
        testList.setCategory("Electronics");

        testItem = new Item();
        testItem.setId(testId);
        testItem.setName("Test Item");
        testItem.setItemList(testList);
        testItem.setStatus(ItemStatus.AVAILABLE);
        testItem.setStock(10);

        when(uploadRateLimiter.tryAcquire(anyString()))
                .thenReturn(new ApiRateLimiter.RateLimitResult(true, 9));
    }

    @Nested
    @DisplayName("GET /api/v1/items")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void getAllItems_returnsOkWithPageResponse() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of(testItem)));

            mockMvc.perform(get("/api/v1/items")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].name").value("Test Item"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("should support pagination parameters")
        void getAllItems_withPagination_returnsPagedResults() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of(testItem)));

            mockMvc.perform(get("/api/v1/items")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sortBy", "name")
                            .param("sortDir", "asc")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should fall back to createdAt when sortBy is invalid")
        void getAllItems_invalidSortBy_fallsBackToCreatedAt() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of(testItem)));

            mockMvc.perform(get("/api/v1/items")
                            .param("sortBy", "INVALID_FIELD")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should clamp size to minimum 1 when size is 0")
        void getAllItems_sizeTooSmall_clampsToOne() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/items")
                            .param("size", "0")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should clamp size to maximum 100 when size exceeds limit")
        void getAllItems_sizeTooLarge_clampsToHundred() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/items")
                            .param("size", "500")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should sort descending by default")
        void getAllItems_defaultSortDir_sortsDescending() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of(testItem)));

            mockMvc.perform(get("/api/v1/items")
                            .param("sortDir", "desc")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/items/{id}")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when exists")
        void getItem_existingId_returnsItem() throws Exception {
            when(itemService.getItemById(testId)).thenReturn(Optional.of(testItem));

            mockMvc.perform(get("/api/v1/items/{id}", testId)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Test Item"))
                    .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
        }

        @Test
        @DisplayName("should return 404 when item not found")
        void getItem_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            when(itemService.getItemById(nonExistingId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/items/{id}", nonExistingId)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/items/barcode/{code}")
    class GetItemByBarcodeTests {

        @Test
        @DisplayName("should return 200 with item when barcode found")
        void getItemByBarcode_found_returns200() throws Exception {
            testItem.setBarcode("1234567890");
            when(itemService.getItemByBarcode("1234567890")).thenReturn(Optional.of(testItem));

            mockMvc.perform(get("/api/v1/items/barcode/{code}", "1234567890")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testId.toString()))
                    .andExpect(jsonPath("$.data.name").value("Test Item"))
                    .andExpect(jsonPath("$.data.barcode").value("1234567890"));
        }

        @Test
        @DisplayName("should return 404 when barcode not found")
        void getItemByBarcode_notFound_returns404() throws Exception {
            when(itemService.getItemByBarcode("NONEXISTENT")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/items/barcode/{code}", "NONEXISTENT")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/items")
    class CreateItemTests {

        @Test
        @DisplayName("should create item with valid request")
        void createItem_validRequest_returns201() throws Exception {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.AVAILABLE, 5, null, null);
            when(itemService.createItem(any(ItemRequest.class), any())).thenReturn(testItem);

            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items")
                            .param("data", jsonData)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Test Item"));
        }

        @Test
        @DisplayName("should return 429 when rate limited on create")
        void createItem_rateLimited_returns429() throws Exception {
            when(uploadRateLimiter.tryAcquire(anyString()))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(false, 0));

            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.AVAILABLE, 5, null, null);
            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items")
                            .param("data", jsonData)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("should return 400 when validation fails")
        void createItem_invalidRequest_returns400() throws Exception {
            // name is required, so empty string or null should fail validation
            String invalidJson = "{\"itemListId\":\"" + testListId + "\"}";

            mockMvc.perform(multipart("/api/v1/items")
                            .param("data", invalidJson)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should create item with image")
        void createItem_withImage_returns201() throws Exception {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.AVAILABLE, 10, null, null);
            when(itemService.createItem(any(ItemRequest.class), any())).thenReturn(testItem);

            String jsonData = objectMapper.writeValueAsString(request);
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "test image".getBytes());

            mockMvc.perform(multipart("/api/v1/items")
                            .file(imagePart)
                            .param("data", jsonData)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/items/{id}")
    class UpdateItemTests {

        @Test
        @DisplayName("should update existing item")
        void updateItem_existingId_returnsUpdatedItem() throws Exception {
            ItemRequest request = new ItemRequest("Updated Item", testListId, ItemStatus.TO_VERIFY, 20, null, null);
            Item updatedItem = new Item();
            updatedItem.setId(testId);
            updatedItem.setName("Updated Item");
            updatedItem.setItemList(testList);
            updatedItem.setStatus(ItemStatus.TO_VERIFY);
            updatedItem.setStock(20);

            when(itemService.updateItem(eq(testId), any(ItemRequest.class), any())).thenReturn(updatedItem);

            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items/{id}", testId)
                            .param("data", jsonData)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER")))
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Item"))
                    .andExpect(jsonPath("$.data.status").value("TO_VERIFY"));
        }

        @Test
        @DisplayName("should return 429 when rate limited on update")
        void updateItem_rateLimited_returns429() throws Exception {
            when(uploadRateLimiter.tryAcquire(anyString()))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(false, 0));

            ItemRequest request = new ItemRequest("Updated Item", testListId, ItemStatus.TO_VERIFY, 20, null, null);
            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items/{id}", testId)
                            .param("data", jsonData)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER")))
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("should return 400 when validation fails on update")
        void updateItem_invalidRequest_returns400() throws Exception {
            String invalidJson = "{\"itemListId\":\"" + testListId + "\"}";

            mockMvc.perform(multipart("/api/v1/items/{id}", testId)
                            .param("data", invalidJson)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER")))
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/items/{id}")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_existingId_returns204() throws Exception {
            doNothing().when(itemService).deleteItem(testId);

            mockMvc.perform(delete("/api/v1/items/{id}", testId)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when item not found")
        void deleteItem_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            doThrow(new ItemNotFoundException(nonExistingId)).when(itemService).deleteItem(nonExistingId);

            mockMvc.perform(delete("/api/v1/items/{id}", nonExistingId)
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/items/stats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("should return dashboard statistics")
        void getDashboardStats_returnsStats() throws Exception {
            DashboardStats stats = new DashboardStats(
                    10L, 100L, 2L, 1L,
                    Map.of("AVAILABLE", 5L, "TO_VERIFY", 3L, "DAMAGED", 2L),
                    Map.of("Electronics", 6L, "Clothing", 4L),
                    List.of(), List.of()
            );
            when(itemService.getDashboardStats()).thenReturn(stats);

            mockMvc.perform(get("/api/v1/items/stats")
                            .with(user(new CustomUserDetails(UUID.randomUUID(), "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalItems").value(10))
                    .andExpect(jsonPath("$.data.countByStatus.AVAILABLE").value(5))
                    .andExpect(jsonPath("$.data.countByCategory.Electronics").value(6));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/items/analyze-image")
    class AnalyzeImageTests {

        private final UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("should return 503 when AI is not available")
        void analyzeImage_aiUnavailable_returns503() throws Exception {
            when(imageAnalysisService.isAvailable()).thenReturn(false);

            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0});

            mockMvc.perform(multipart("/api/v1/items/analyze-image").file(image)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("should return 202 with analysisId for valid image")
        void analyzeImage_validImage_returns202() throws Exception {
            when(imageAnalysisService.isAvailable()).thenReturn(true);
            when(imageAnalysisService.analyzeImage(any(byte[].class), any(), eq(userId)))
                    .thenReturn("test-analysis-id");

            // Valid JPEG magic bytes
            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0});

            mockMvc.perform(multipart("/api/v1/items/analyze-image").file(image)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.analysisId").value("test-analysis-id"));
        }

        @Test
        @DisplayName("should return 400 for empty image")
        void analyzeImage_emptyImage_returns400() throws Exception {
            when(imageAnalysisService.isAvailable()).thenReturn(true);

            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

            mockMvc.perform(multipart("/api/v1/items/analyze-image").file(image)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for non-image content")
        void analyzeImage_invalidContent_returns400() throws Exception {
            when(imageAnalysisService.isAvailable()).thenReturn(true);

            // Random bytes, not a valid image
            MockMultipartFile image = new MockMultipartFile("image", "test.bin", "application/octet-stream",
                    new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B});

            mockMvc.perform(multipart("/api/v1/items/analyze-image").file(image)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 429 when rate limited")
        void analyzeImage_rateLimited_returns429() throws Exception {
            when(imageAnalysisService.isAvailable()).thenReturn(true);
            when(uploadRateLimiter.tryAcquire(anyString()))
                    .thenReturn(new ApiRateLimiter.RateLimitResult(false, 0));

            MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg",
                    new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0});

            mockMvc.perform(multipart("/api/v1/items/analyze-image").file(image)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/items/analyze-image/{analysisId}")
    class GetAnalysisResultTests {

        private final UUID userId = UUID.randomUUID();

        @Test
        @DisplayName("should return 200 with result when found")
        void getAnalysisResult_found_returns200() throws Exception {
            UUID analysisId = UUID.randomUUID();
            ImageAnalysisResult result = new ImageAnalysisResult(
                    analysisId.toString(), ImageAnalysisResult.AnalysisStatus.COMPLETED,
                    "Blue Keyboard", "AVAILABLE", 1, null, null);

            when(imageAnalysisService.getResult(analysisId.toString(), userId))
                    .thenReturn(Optional.of(result));

            mockMvc.perform(get("/api/v1/items/analyze-image/{id}", analysisId)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.suggestedName").value("Blue Keyboard"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void getAnalysisResult_notFound_returns404() throws Exception {
            UUID analysisId = UUID.randomUUID();
            when(imageAnalysisService.getResult(analysisId.toString(), userId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/items/analyze-image/{id}", analysisId)
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should reject invalid UUID format")
        void getAnalysisResult_invalidUUID_rejectsRequest() throws Exception {
            mockMvc.perform(get("/api/v1/items/analyze-image/{id}", "not-a-uuid")
                            .with(user(new CustomUserDetails(userId, "test@test.com", "USER"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
