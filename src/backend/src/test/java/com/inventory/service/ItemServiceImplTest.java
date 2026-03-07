package com.inventory.service;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ItemStatus;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.security.SecurityUtils;
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
import java.util.Objects;
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

    @Mock
    private ItemListRepository itemListRepository;

    @Mock
    private CustomFieldValidator customFieldValidator;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item testItem;
    private ItemList testList;
    private UUID testId;
    private UUID testListId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testListId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        User testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");

        testList = new ItemList();
        testList.setId(testListId);
        testList.setName("Test List");
        testList.setCategory("Electronics");
        testList.setUser(testUser);

        testItem = new Item();
        testItem.setId(testId);
        testItem.setName("Test Item");
        testItem.setItemList(testList);
        testItem.setStatus(ItemStatus.IN_STOCK);
        testItem.setStock(10);
    }

    @Nested
    @DisplayName("getAllItems")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void getAllItems_returnsPageOfItems() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null);
            Page<Item> expectedPage = new PageImpl<>(Objects.requireNonNull(List.of(testItem), "List of items not found"));

            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
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
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));

            Optional<Item> result = itemService.getItemById(testId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testId);
            assertThat(result.get().getName()).isEqualTo("Test Item");
        }

        @Test
        @DisplayName("should throw exception when item not found")
        void getItemById_nonExistingId_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(itemRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            Optional<Item> result = itemService.getItemById(nonExistingId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        @Test
        @DisplayName("should create item with valid request")
        void createItem_validRequest_createsItem() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.IN_STOCK, 5, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            Item result = itemService.createItem(request, null);

            assertThat(result.getName()).isEqualTo("New Item");
            assertThat(result.getItemList()).isEqualTo(testList);
            assertThat(result.getStatus()).isEqualTo(ItemStatus.IN_STOCK);
            assertThat(result.getStock()).isEqualTo(5);
            verify(itemRepository).save(any(Item.class));
        }

        @Test
        @DisplayName("should create item with default status when not provided")
        void createItem_noStatus_usesDefaultStatus() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, null, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.IN_STOCK);
            assertThat(result.getStock()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception when list not found")
        void createItem_listNotFound_throwsException() {
            UUID nonExistingListId = UUID.randomUUID();
            ItemRequest request = new ItemRequest("New Item", nonExistingListId, ItemStatus.IN_STOCK, 5, null);
            when(itemListRepository.findById(nonExistingListId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.createItem(request, null))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should store image data when provided")
        void createItem_withImage_storesImageData() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.OUT_OF_STOCK, 10, null);
            // JPEG magic bytes (FF D8 FF) followed by dummy data
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", jpegBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
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
            ItemRequest request = new ItemRequest("Updated Name", testListId, ItemStatus.LOW_STOCK, 20, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getItemList()).isEqualTo(testList);
            assertThat(result.getStatus()).isEqualTo(ItemStatus.LOW_STOCK);
            assertThat(result.getStock()).isEqualTo(20);
        }

        @Test
        @DisplayName("should keep existing status when not provided in request")
        void updateItem_noStatus_keepsExistingStatus() throws IOException {
            testItem.setStatus(ItemStatus.OUT_OF_STOCK);
            ItemRequest request = new ItemRequest("Updated Name", testListId, null, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.OUT_OF_STOCK);
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item")
        void deleteItem_existingId_deletesItem() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            doNothing().when(itemRepository).delete(testItem);

            itemService.deleteItem(testId);

            verify(itemRepository).delete(testItem);
        }

        @Test
        @DisplayName("should throw exception when item not found")
        void deleteItem_nonExistingId_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(itemRepository.findById(nonExistingId)).thenReturn(Optional.empty());

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
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.count()).thenReturn(10L);
            when(itemRepository.sumStock()).thenReturn(100L);
            when(itemRepository.countLowStock(5)).thenReturn(2L);
            when(itemRepository.countOutOfStock()).thenReturn(1L);
            when(itemRepository.getListsOverview()).thenReturn(List.of());
            when(itemRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of());
            when(itemRepository.countByStatus()).thenReturn(List.of(
                    new Object[]{ItemStatus.IN_STOCK, 5L},
                    new Object[]{ItemStatus.LOW_STOCK, 3L},
                    new Object[]{ItemStatus.OUT_OF_STOCK, 2L}
            ));
            when(itemRepository.countByCategory()).thenReturn(List.of(
                    new Object[]{"Electronics", 6L},
                    new Object[]{"Clothing", 4L}
            ));

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(10L);
            assertThat(stats.totalQuantity()).isEqualTo(100L);
            assertThat(stats.lowStockCount()).isEqualTo(2L);
            assertThat(stats.outOfStockCount()).isEqualTo(1L);
            assertThat(stats.countByStatus()).containsEntry("IN_STOCK", 5L);
            assertThat(stats.countByStatus()).containsEntry("LOW_STOCK", 3L);
            assertThat(stats.countByCategory()).containsEntry("Electronics", 6L);
        }
    }
}
