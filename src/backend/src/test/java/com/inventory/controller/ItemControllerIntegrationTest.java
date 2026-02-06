package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ItemRequest;
import com.inventory.enums.ItemStatus;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.repository.ItemListRepository;
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

    @Autowired
    private ItemListRepository itemListRepository;

    private ItemList testList;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        itemListRepository.deleteAll();

        testList = new ItemList();
        testList.setName("Test List");
        testList.setCategory("Electronics");
        testList = itemListRepository.save(testList);
    }

    @Nested
    @DisplayName("Create and Retrieve Item Flow")
    class CreateAndRetrieveTests {

        @Test
        @DisplayName("should create item and retrieve it")
        void createAndRetrieveItem_fullFlow() throws Exception {
            ItemRequest request = new ItemRequest("Integration Test Item", testList.getId(), ItemStatus.TO_PREPARE, 5);

            String jsonData = objectMapper.writeValueAsString(request);

            MvcResult createResult = mockMvc.perform(multipart("/api/v1/items")
                            .param("data", jsonData))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Integration Test Item"))
                    .andExpect(jsonPath("$.itemListId").value(testList.getId().toString()))
                    .andExpect(jsonPath("$.status").value("TO_PREPARE"))
                    .andExpect(jsonPath("$.stock").value(5))
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
            ItemRequest request = new ItemRequest("Item With Image", testList.getId(), ItemStatus.TO_PREPARE, 10);

            String jsonData = objectMapper.writeValueAsString(request);
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "fake image content".getBytes());

            MvcResult result = mockMvc.perform(multipart("/api/v1/items")
                            .file(imagePart)
                            .param("data", jsonData))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.hasImage").value(true))
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
            Item item = createTestItem("Original Name", testList, ItemStatus.TO_PREPARE, 5);

            ItemRequest updateRequest = new ItemRequest("Updated Name", testList.getId(), ItemStatus.READY, 15);
            String jsonData = objectMapper.writeValueAsString(updateRequest);

            mockMvc.perform(multipart("/api/v1/items/{id}", item.getId())
                            .param("data", jsonData)
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Name"))
                    .andExpect(jsonPath("$.status").value("READY"))
                    .andExpect(jsonPath("$.stock").value(15));

            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getName()).isEqualTo("Updated Name");
            assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.READY);
            assertThat(updatedItem.getStock()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Delete Item Flow")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_fullFlow() throws Exception {
            Item item = createTestItem("To Delete", testList, ItemStatus.TO_PREPARE, 0);

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
            createTestItem("Item 1", testList, ItemStatus.TO_PREPARE, 5);
            createTestItem("Item 2", testList, ItemStatus.READY, 10);
            createTestItem("Item 3", testList, ItemStatus.TO_PREPARE, 3);

            mockMvc.perform(get("/api/v1/items")
                            .param("status", "TO_PREPARE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].status").value("TO_PREPARE"));
        }

        @Test
        @DisplayName("should filter items by list")
        void searchItems_byList() throws Exception {
            ItemList anotherList = new ItemList();
            anotherList.setName("Another List");
            anotherList.setCategory("Clothing");
            anotherList = itemListRepository.save(anotherList);

            createTestItem("Item 1", testList, ItemStatus.TO_PREPARE, 5);
            createTestItem("Item 2", testList, ItemStatus.READY, 10);
            createTestItem("Item 3", anotherList, ItemStatus.TO_PREPARE, 3);

            mockMvc.perform(get("/api/v1/items")
                            .param("itemListId", testList.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should search items by name")
        void searchItems_bySearch() throws Exception {
            createTestItem("Laptop", testList, ItemStatus.TO_PREPARE, 5);
            createTestItem("Phone", testList, ItemStatus.TO_PREPARE, 10);
            createTestItem("T-Shirt", testList, ItemStatus.TO_PREPARE, 20);

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
            ItemList clothingList = new ItemList();
            clothingList.setName("Clothing List");
            clothingList.setCategory("Clothing");
            clothingList = itemListRepository.save(clothingList);

            createTestItem("Item 1", testList, ItemStatus.TO_PREPARE, 5);
            createTestItem("Item 2", testList, ItemStatus.READY, 10);
            createTestItem("Item 3", clothingList, ItemStatus.TO_PREPARE, 3);
            createTestItem("Item 4", clothingList, ItemStatus.PENDING, 0);

            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalItems").value(4))
                    .andExpect(jsonPath("$.countByStatus.TO_PREPARE").value(2))
                    .andExpect(jsonPath("$.countByStatus.READY").value(1))
                    .andExpect(jsonPath("$.countByStatus.PENDING").value(1))
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

    private Item createTestItem(String name, ItemList list, ItemStatus status, Integer stock) {
        Item item = new Item();
        item.setName(name);
        item.setItemList(list);
        item.setStatus(status);
        item.setStock(stock);
        return itemRepository.save(item);
    }
}
