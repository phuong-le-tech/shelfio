package com.inventory.service;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.repository.ItemRepository;
import com.inventory.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemServiceImpl Tests")
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item testItem;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testItem = new Item();
        testItem.setId(testId);
        testItem.setName("Test Item");
        testItem.setCategory("Electronics");
        testItem.setStatus(ItemStatus.IN_STOCK);
    }

    @Nested
    @DisplayName("getAllItems")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void getAllItems_returnsPageOfItems() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null);
            Page<Item> expectedPage = new PageImpl<>(List.of(testItem));

            when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            Page<Item> result = itemService.getAllItems(pageable, criteria);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Test Item");
            verify(itemRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    @Nested
    @DisplayName("getItemById")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when exists")
        void getItemById_existingId_returnsItem() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));

            Item result = itemService.getItemById(testId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testId);
            assertThat(result.getName()).isEqualTo("Test Item");
        }

        @Test
        @DisplayName("should throw exception when item not found")
        void getItemById_nonExistingId_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(itemRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(nonExistingId))
                    .isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("should create item with valid request")
        void createItem_validRequest_createsItem() throws IOException {
            ItemRequest request = new ItemRequest("New Item", "Category", ItemStatus.IN_STOCK);
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            Item result = itemService.createItem(request, null);

            assertThat(result.getName()).isEqualTo("New Item");
            assertThat(result.getCategory()).isEqualTo("Category");
            assertThat(result.getStatus()).isEqualTo(ItemStatus.IN_STOCK);
            verify(itemRepository).save(any(Item.class));
        }

        @Test
        @DisplayName("should create item with default status when not provided")
        void createItem_noStatus_usesDefaultStatus() throws IOException {
            ItemRequest request = new ItemRequest("New Item", "Category", null);
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.IN_STOCK);
        }

        @Test
        @DisplayName("should store image data when provided")
        void createItem_withImage_storesImageData() throws IOException {
            ItemRequest request = new ItemRequest("New Item", "Category", ItemStatus.IN_STOCK);
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", "test image content".getBytes());

            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, image);

            assertThat(result.getImageData()).isNotNull();
            assertThat(result.getContentType()).isEqualTo("image/jpeg");
            verify(itemRepository).save(any(Item.class));
        }
    }

    @Nested
    @DisplayName("updateItem")
    class UpdateItemTests {

        @Test
        @DisplayName("should update existing item")
        void updateItem_existingId_updatesItem() throws IOException {
            ItemRequest request = new ItemRequest("Updated Name", "Updated Category", ItemStatus.SOLD);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getCategory()).isEqualTo("Updated Category");
            assertThat(result.getStatus()).isEqualTo(ItemStatus.SOLD);
        }

        @Test
        @DisplayName("should keep existing status when not provided in request")
        void updateItem_noStatus_keepsExistingStatus() throws IOException {
            testItem.setStatus(ItemStatus.DAMAGED);
            ItemRequest request = new ItemRequest("Updated Name", "Updated Category", null);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.DAMAGED);
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_existingId_deletesItem() {
            when(itemRepository.existsById(testId)).thenReturn(true);
            doNothing().when(itemRepository).deleteById(testId);

            itemService.deleteItem(testId);

            verify(itemRepository).deleteById(testId);
        }

        @Test
        @DisplayName("should throw exception when item not found")
        void deleteItem_nonExistingId_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(itemRepository.existsById(nonExistingId)).thenReturn(false);

            assertThatThrownBy(() -> itemService.deleteItem(nonExistingId))
                    .isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("should return dashboard statistics")
        void getDashboardStats_returnsStats() {
            when(itemRepository.count()).thenReturn(10L);
            when(itemRepository.countByStatus()).thenReturn(List.of(
                    new Object[]{ItemStatus.IN_STOCK, 5L},
                    new Object[]{ItemStatus.SOLD, 3L},
                    new Object[]{ItemStatus.OUT_OF_STOCK, 2L}
            ));
            when(itemRepository.countByCategory()).thenReturn(List.of(
                    new Object[]{"Electronics", 6L},
                    new Object[]{"Clothing", 4L}
            ));

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(10L);
            assertThat(stats.countByStatus()).containsEntry("IN_STOCK", 5L);
            assertThat(stats.countByStatus()).containsEntry("SOLD", 3L);
            assertThat(stats.countByCategory()).containsEntry("Electronics", 6L);
        }
    }
}
