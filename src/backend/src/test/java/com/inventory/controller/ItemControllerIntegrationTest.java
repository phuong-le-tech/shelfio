package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ItemRequest;
import com.inventory.enums.ItemStatus;
import com.inventory.model.Item;
import com.inventory.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ItemController Integration Tests")
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
    }

    @Nested
    @DisplayName("Create and Retrieve Item Flow")
    class CreateAndRetrieveTests {

        @Test
        @DisplayName("should create item and retrieve it")
        void createAndRetrieveItem_fullFlow() throws Exception {
            ItemRequest request = new ItemRequest("Integration Test Item", "Test Category", ItemStatus.IN_STOCK);

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(request));

            MvcResult createResult = mockMvc.perform(multipart("/api/v1/items/")
                            .file(dataPart))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Integration Test Item"))
                    .andExpect(jsonPath("$.category").value("Test Category"))
                    .andExpect(jsonPath("$.status").value("IN_STOCK"))
                    .andReturn();

            String responseJson = createResult.getResponse().getContentAsString();
            String itemId = objectMapper.readTree(responseJson).get("id").asText();

            mockMvc.perform(get("/api/v1/items/{id}", itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(itemId))
                    .andExpect(jsonPath("$.name").value("Integration Test Item"));

            assertThat(itemRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create item with image")
        void createItemWithImage_fullFlow() throws Exception {
            ItemRequest request = new ItemRequest("Item With Image", "Category", ItemStatus.IN_STOCK);

            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(request));
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "fake image content".getBytes());

            MvcResult result = mockMvc.perform(multipart("/api/v1/items/")
                            .file(dataPart)
                            .file(imagePart))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.imageBase64").isNotEmpty())
                    .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                    .andReturn();

            String itemId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
            Item savedItem = itemRepository.findById(UUID.fromString(itemId)).orElseThrow();
            assertThat(savedItem.getImageData()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Update Item Flow")
    class UpdateItemTests {

        @Test
        @DisplayName("should update existing item")
        void updateItem_fullFlow() throws Exception {
            Item item = new Item();
            item.setName("Original Name");
            item.setCategory("Original Category");
            item.setStatus(ItemStatus.IN_STOCK);
            item = itemRepository.save(item);

            ItemRequest updateRequest = new ItemRequest("Updated Name", "Updated Category", ItemStatus.SOLD);
            MockMultipartFile dataPart = new MockMultipartFile(
                    "data", "", "application/json",
                    objectMapper.writeValueAsBytes(updateRequest));

            mockMvc.perform(multipart("/api/v1/items/{id}", item.getId())
                            .file(dataPart)
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"))
                    .andExpect(jsonPath("$.category").value("Updated Category"))
                    .andExpect(jsonPath("$.status").value("SOLD"));

            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getName()).isEqualTo("Updated Name");
            assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.SOLD);
        }
    }

    @Nested
    @DisplayName("Delete Item Flow")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_fullFlow() throws Exception {
            Item item = new Item();
            item.setName("To Delete");
            item.setCategory("Category");
            item.setStatus(ItemStatus.IN_STOCK);
            item = itemRepository.save(item);

            assertThat(itemRepository.count()).isEqualTo(1);

            mockMvc.perform(delete("/api/v1/items/{id}", item.getId()))
                    .andExpect(status().isNoContent());

            assertThat(itemRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return 404 for non-existing item")
        void deleteItem_nonExisting_returns404() throws Exception {
            UUID nonExistingId = UUID.randomUUID();

            mockMvc.perform(delete("/api/v1/items/{id}", nonExistingId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Search and Filter Items")
    class SearchItemsTests {

        @Test
        @DisplayName("should filter items by status")
        void searchItems_byStatus() throws Exception {
            createTestItem("Item 1", "Electronics", ItemStatus.IN_STOCK);
            createTestItem("Item 2", "Electronics", ItemStatus.SOLD);
            createTestItem("Item 3", "Clothing", ItemStatus.IN_STOCK);

            mockMvc.perform(get("/api/v1/items")
                            .param("status", "IN_STOCK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].status").value("IN_STOCK"));
        }

        @Test
        @DisplayName("should filter items by category")
        void searchItems_byCategory() throws Exception {
            createTestItem("Item 1", "Electronics", ItemStatus.IN_STOCK);
            createTestItem("Item 2", "Electronics", ItemStatus.SOLD);
            createTestItem("Item 3", "Clothing", ItemStatus.IN_STOCK);

            mockMvc.perform(get("/api/v1/items")
                            .param("category", "Electronics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should search items by name")
        void searchItems_bySearch() throws Exception {
            createTestItem("Laptop", "Electronics", ItemStatus.IN_STOCK);
            createTestItem("Phone", "Electronics", ItemStatus.IN_STOCK);
            createTestItem("T-Shirt", "Clothing", ItemStatus.IN_STOCK);

            mockMvc.perform(get("/api/v1/items")
                            .param("search", "Laptop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Laptop"));
        }
    }

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardStatsTests {

        @Test
        @DisplayName("should return correct statistics")
        void dashboardStats_reflectsData() throws Exception {
            createTestItem("Item 1", "Electronics", ItemStatus.IN_STOCK);
            createTestItem("Item 2", "Electronics", ItemStatus.SOLD);
            createTestItem("Item 3", "Clothing", ItemStatus.IN_STOCK);
            createTestItem("Item 4", "Clothing", ItemStatus.OUT_OF_STOCK);

            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(4))
                    .andExpect(jsonPath("$.countByStatus.IN_STOCK").value(2))
                    .andExpect(jsonPath("$.countByStatus.SOLD").value(1))
                    .andExpect(jsonPath("$.countByStatus.OUT_OF_STOCK").value(1))
                    .andExpect(jsonPath("$.countByCategory.Electronics").value(2))
                    .andExpect(jsonPath("$.countByCategory.Clothing").value(2));
        }

        @Test
        @DisplayName("should return empty stats when no items")
        void dashboardStats_emptyDatabase() throws Exception {
            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(0));
        }
    }

    private Item createTestItem(String name, String category, ItemStatus status) {
        Item item = new Item();
        item.setName(name);
        item.setCategory(category);
        item.setStatus(status);
        return itemRepository.save(item);
    }
}
