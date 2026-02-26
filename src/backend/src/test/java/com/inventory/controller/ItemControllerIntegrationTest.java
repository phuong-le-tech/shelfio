package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.ItemRequest;
import com.inventory.enums.ItemStatus;
import com.inventory.enums.Role;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.UserRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.security.CustomUserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
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

    @Autowired
    private UserRepository userRepository;

    private ItemList testList;
    private User testUser;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll();
        itemListRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setRole(Role.USER);
        testUser = userRepository.save(testUser);

        testList = new ItemList();
        testList.setName("Test List");
        testList.setCategory("Electronics");
        testList.setUser(testUser);
        testList = itemListRepository.save(testList);

        // Set up SecurityContext so service-layer auth checks pass
        CustomUserDetails userDetails = new CustomUserDetails(
                testUser.getId(), testUser.getEmail(), testUser.getRole().name());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Create and Retrieve Item Flow")
    class CreateAndRetrieveTests {

        @Test
        @DisplayName("should create item and retrieve it")
        void createAndRetrieveItem_fullFlow() throws Exception {
            ItemRequest request = new ItemRequest("Integration Test Item", testList.getId(), ItemStatus.IN_STOCK, 5, null);

            String jsonData = objectMapper.writeValueAsString(request);

            MvcResult createResult = mockMvc.perform(multipart("/api/v1/items")
                            .param("data", jsonData))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("Integration Test Item"))
                    .andExpect(jsonPath("$.data.itemListId").value(testList.getId().toString()))
                    .andExpect(jsonPath("$.data.status").value("IN_STOCK"))
                    .andExpect(jsonPath("$.data.stock").value(5))
                    .andReturn();

            String responseJson = createResult.getResponse().getContentAsString();
            String itemId = objectMapper.readTree(responseJson).get("data").get("id").asText();

            mockMvc.perform(get("/api/v1/items/{id}", itemId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(itemId))
                    .andExpect(jsonPath("$.data.name").value("Integration Test Item"));

            assertThat(itemRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("should create item with image")
        void createItemWithImage_fullFlow() throws Exception {
            ItemRequest request = new ItemRequest("Item With Image", testList.getId(), ItemStatus.IN_STOCK, 10, null);

            String jsonData = objectMapper.writeValueAsString(request);
            // JPEG magic bytes (FF D8 FF) followed by dummy data
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile imagePart = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", jpegBytes);

            MvcResult result = mockMvc.perform(multipart("/api/v1/items")
                            .file(imagePart)
                            .param("data", jsonData))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.hasImage").value(true))
                    .andExpect(jsonPath("$.data.contentType").value("image/jpeg"))
                    .andReturn();

            String itemId = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asText();
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
            Item item = createTestItem("Original Name", testList, ItemStatus.IN_STOCK, 5);

            ItemRequest updateRequest = new ItemRequest("Updated Name", testList.getId(), ItemStatus.LOW_STOCK, 15, null);
            String jsonData = objectMapper.writeValueAsString(updateRequest);

            mockMvc.perform(multipart("/api/v1/items/{id}", item.getId())
                            .param("data", jsonData)
                            .with(req -> {
                                req.setMethod("PATCH");
                                return req;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Name"))
                    .andExpect(jsonPath("$.data.status").value("LOW_STOCK"))
                    .andExpect(jsonPath("$.data.stock").value(15));

            Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
            assertThat(updatedItem.getName()).isEqualTo("Updated Name");
            assertThat(updatedItem.getStatus()).isEqualTo(ItemStatus.LOW_STOCK);
            assertThat(updatedItem.getStock()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Delete Item Flow")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_fullFlow() throws Exception {
            Item testItem = createTestItem("Test Item", testList, ItemStatus.IN_STOCK, 10);

            assertThat(itemRepository.count()).isEqualTo(1);

            mockMvc.perform(delete("/api/v1/items/{id}", testItem.getId()))
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
            createTestItem("Item 1", testList, ItemStatus.IN_STOCK, 5);
            createTestItem("Item 2", testList, ItemStatus.LOW_STOCK, 10);
            createTestItem("Item 3", testList, ItemStatus.IN_STOCK, 3);

            mockMvc.perform(get("/api/v1/items")
                            .param("status", "IN_STOCK"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.content[0].status").value("IN_STOCK"));
        }

        @Test
        @DisplayName("should filter items by list")
        void searchItems_byList() throws Exception {
            ItemList anotherList = new ItemList();
            anotherList.setName("Another List");
            anotherList.setCategory("Clothing");
            anotherList.setUser(testUser);
            anotherList = itemListRepository.save(anotherList);

            createTestItem("Item 1", testList, ItemStatus.IN_STOCK, 5);
            createTestItem("Item 2", testList, ItemStatus.LOW_STOCK, 10);
            createTestItem("Item 3", anotherList, ItemStatus.IN_STOCK, 3);

            mockMvc.perform(get("/api/v1/items")
                            .param("itemListId", testList.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(2));
        }

        @Test
        @DisplayName("should search items by name")
        void searchItems_bySearch() throws Exception {
            createTestItem("Laptop", testList, ItemStatus.IN_STOCK, 5);
            createTestItem("Phone", testList, ItemStatus.IN_STOCK, 10);
            createTestItem("T-Shirt", testList, ItemStatus.IN_STOCK, 20);

            mockMvc.perform(get("/api/v1/items")
                            .param("search", "Laptop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.content[0].name").value("Laptop"));
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
            clothingList.setUser(testUser);
            clothingList = itemListRepository.save(clothingList);

            createTestItem("Item 1", testList, ItemStatus.IN_STOCK, 5);
            createTestItem("Item 2", testList, ItemStatus.LOW_STOCK, 10);
            createTestItem("Item 3", clothingList, ItemStatus.IN_STOCK, 3);
            createTestItem("Item 4", clothingList, ItemStatus.OUT_OF_STOCK, 0);

            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalItems").value(4))
                    .andExpect(jsonPath("$.data.countByStatus.OUT_OF_STOCK").value(1))
                    .andExpect(jsonPath("$.data.countByCategory.Electronics").value(2))
                    .andExpect(jsonPath("$.data.countByCategory.Clothing").value(2));
        }

        @Test
        @DisplayName("should return empty stats when no items")
        void dashboardStats_emptyDatabase() throws Exception {
            mockMvc.perform(get("/api/v1/items/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalItems").value(0));
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
