package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.service.IItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@DisplayName("ItemController Tests")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IItemService itemService;

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
        testItem.setStatus(ItemStatus.TO_PREPARE);
        testItem.setStock(10);
    }

    @Nested
    @DisplayName("GET /api/v1/items")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void getAllItems_returnsOkWithPageResponse() throws Exception {
            when(itemService.getAllItems(any(Pageable.class), any(ItemSearchCriteria.class)))
                    .thenReturn(new PageImpl<>(List.of(testItem)));

            mockMvc.perform(get("/api/v1/items"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].name").value("Test Item"))
                    .andExpect(jsonPath("$.totalElements").value(1));
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
                            .param("sortDir", "asc"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/items/{id}")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when exists")
        void getItem_existingId_returnsItem() throws Exception {
            when(itemService.getItemById(testId)).thenReturn(testItem);

            mockMvc.perform(get("/api/v1/items/{id}", testId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(testId.toString()))
                    .andExpect(jsonPath("$.name").value("Test Item"))
                    .andExpect(jsonPath("$.status").value("TO_PREPARE"));
        }

        @Test
        @DisplayName("should return 404 when item not found")
        void getItem_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            when(itemService.getItemById(nonExistingId))
                    .thenThrow(new ItemNotFoundException(nonExistingId));

            mockMvc.perform(get("/api/v1/items/{id}", nonExistingId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/items")
    class CreateItemTests {

        @Test
        @DisplayName("should create item with valid request")
        void createItem_validRequest_returns201() throws Exception {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.TO_PREPARE, 5);
            when(itemService.createItem(any(ItemRequest.class), any())).thenReturn(testItem);

            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items")
                            .param("data", jsonData))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Test Item"));
        }

        @Test
        @DisplayName("should create item with image")
        void createItem_withImage_returns201() throws Exception {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.TO_PREPARE, 10);
            when(itemService.createItem(any(ItemRequest.class), any())).thenReturn(testItem);

            String jsonData = objectMapper.writeValueAsString(request);
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "test image".getBytes());

            mockMvc.perform(multipart("/api/v1/items")
                            .file(imagePart)
                            .param("data", jsonData))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/items/{id}")
    class UpdateItemTests {

        @Test
        @DisplayName("should update existing item")
        void updateItem_existingId_returnsUpdatedItem() throws Exception {
            ItemRequest request = new ItemRequest("Updated Item", testListId, ItemStatus.READY, 20);
            Item updatedItem = new Item();
            updatedItem.setId(testId);
            updatedItem.setName("Updated Item");
            updatedItem.setItemList(testList);
            updatedItem.setStatus(ItemStatus.READY);
            updatedItem.setStock(20);

            when(itemService.updateItem(eq(testId), any(ItemRequest.class), any())).thenReturn(updatedItem);

            String jsonData = objectMapper.writeValueAsString(request);

            mockMvc.perform(multipart("/api/v1/items/{id}", testId)
                            .param("data", jsonData)
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Item"))
                    .andExpect(jsonPath("$.status").value("READY"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/items/{id}")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_existingId_returns204() throws Exception {
            doNothing().when(itemService).deleteItem(testId);

            mockMvc.perform(delete("/api/v1/items/{id}", testId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("should return 404 when item not found")
        void deleteItem_nonExistingId_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();
            doThrow(new ItemNotFoundException(nonExistingId)).when(itemService).deleteItem(nonExistingId);

            mockMvc.perform(delete("/api/v1/items/{id}", nonExistingId))
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
                    10L,
                    Map.of("TO_PREPARE", 5L, "READY", 3L, "PENDING", 2L),
                    Map.of("Electronics", 6L, "Clothing", 4L)
            );
            when(itemService.getDashboardStats()).thenReturn(stats);

            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(10))
                    .andExpect(jsonPath("$.countByStatus.TO_PREPARE").value(5))
                    .andExpect(jsonPath("$.countByCategory.Electronics").value(6));
        }
    }
}
