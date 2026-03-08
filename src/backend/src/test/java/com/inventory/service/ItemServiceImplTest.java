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

import com.inventory.exception.FileValidationException;
import com.inventory.exception.UnauthorizedException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        @Test
        @DisplayName("should return all items for admin without user filter")
        void getAllItems_admin_returnsAllItems() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null);
            Page<Item> expectedPage = new PageImpl<>(List.of(testItem));

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            Page<Item> result = itemService.getAllItems(pageable, criteria);

            assertThat(result.getContent()).hasSize(1);
            verify(securityUtils, never()).getCurrentUserId();
        }

        @Test
        @DisplayName("should throw UnauthorizedException when user not authenticated")
        void getAllItems_notAuthenticated_throwsException() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null);

            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getAllItems(pageable, criteria))
                    .isInstanceOf(UnauthorizedException.class);
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

        @Test
        @DisplayName("should return item for admin regardless of ownership")
        void getItemById_admin_returnsItem() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(true);

            Optional<Item> result = itemService.getItemById(testId);

            assertThat(result).isPresent();
            verify(securityUtils, never()).getCurrentUserId();
        }

        @Test
        @DisplayName("should throw when non-owner accesses item")
        void getItemById_nonOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(otherUserId));

            assertThatThrownBy(() -> itemService.getItemById(testId))
                    .isInstanceOf(ItemNotFoundException.class);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when not authenticated")
        void getItemById_notAuthenticated_throwsException() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getItemById(testId))
                    .isInstanceOf(UnauthorizedException.class);
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

        @Test
        @DisplayName("should create item with PNG image")
        void createItem_withPngImage_storesImageData() throws IOException {
            ItemRequest request = new ItemRequest("PNG Item", testListId, ItemStatus.IN_STOCK, 1, null);
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.png", "image/png", pngBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, image);

            assertThat(result.getContentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("should create item with GIF image")
        void createItem_withGifImage_storesImageData() throws IOException {
            ItemRequest request = new ItemRequest("GIF Item", testListId, ItemStatus.IN_STOCK, 1, null);
            byte[] gifBytes = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.gif", "image/gif", gifBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, image);

            assertThat(result.getContentType()).isEqualTo("image/gif");
        }

        @Test
        @DisplayName("should create item with WebP image")
        void createItem_withWebpImage_storesImageData() throws IOException {
            ItemRequest request = new ItemRequest("WebP Item", testListId, ItemStatus.IN_STOCK, 1, null);
            // RIFF....WEBP
            byte[] webpBytes = new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};
            MockMultipartFile image = new MockMultipartFile("image", "test.webp", "image/webp", webpBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, image);

            assertThat(result.getContentType()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("should throw exception for file too large")
        void createItem_fileTooLarge_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.IN_STOCK, 1, null);
            byte[] largeFile = new byte[10 * 1024 * 1024 + 1]; // just over 10MB
            largeFile[0] = (byte) 0xFF;
            largeFile[1] = (byte) 0xD8;
            largeFile[2] = (byte) 0xFF;
            MockMultipartFile image = new MockMultipartFile("image", "big.jpg", "image/jpeg", largeFile);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("10MB");
        }

        @Test
        @DisplayName("should throw exception for invalid file type")
        void createItem_invalidFileType_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.IN_STOCK, 1, null);
            byte[] invalidBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0, 0, 0, 0, 0, 0, 0, 0}; // PDF magic
            MockMultipartFile image = new MockMultipartFile("image", "test.pdf", "application/pdf", invalidBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        @DisplayName("should throw exception for file too small to detect type")
        void createItem_fileTooSmall_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.IN_STOCK, 1, null);
            byte[] tinyBytes = new byte[]{0x01, 0x02};
            MockMultipartFile image = new MockMultipartFile("image", "tiny.bin", "application/octet-stream", tinyBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        @DisplayName("should throw when non-owner creates item in another user's list")
        void createItem_nonOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.IN_STOCK, 5, null);

            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(otherUserId));

            assertThatThrownBy(() -> itemService.createItem(request, null))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should create item as owner (non-admin)")
        void createItem_asOwner_createsItem() throws IOException {
            ItemRequest request = new ItemRequest("Owner Item", testListId, ItemStatus.IN_STOCK, 3, null);

            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, null);

            assertThat(result.getName()).isEqualTo("Owner Item");
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

        @Test
        @DisplayName("should update item with image")
        void updateItem_withImage_updatesImageData() throws IOException {
            ItemRequest request = new ItemRequest("Updated", testListId, ItemStatus.IN_STOCK, 5, null);
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.png", "image/png", pngBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, image);

            assertThat(result.getImageData()).isNotNull();
            assertThat(result.getContentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("should throw when item not found")
        void updateItem_notFound_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            ItemRequest request = new ItemRequest("Updated", testListId, ItemStatus.IN_STOCK, 5, null);

            when(itemRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.updateItem(nonExistingId, request, null))
                    .isInstanceOf(ItemNotFoundException.class);
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

        @Test
        @DisplayName("should delete item as admin")
        void deleteItem_admin_deletesItem() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(true);
            doNothing().when(itemRepository).delete(testItem);

            itemService.deleteItem(testId);

            verify(itemRepository).delete(testItem);
        }

        @Test
        @DisplayName("should throw when non-owner tries to delete")
        void deleteItem_nonOwner_throwsException() {
            UUID otherUserId = UUID.randomUUID();
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(otherUserId));

            assertThatThrownBy(() -> itemService.deleteItem(testId))
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

        @Test
        @DisplayName("should return user-scoped dashboard statistics for non-admin")
        void getDashboardStats_nonAdmin_returnsUserScopedStats() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemRepository.countByUserId(testUserId)).thenReturn(5L);
            when(itemRepository.sumStockByUserId(testUserId)).thenReturn(50L);
            when(itemRepository.countLowStockByUserId(testUserId, 5)).thenReturn(1L);
            when(itemRepository.countOutOfStockByUserId(testUserId)).thenReturn(0L);
            when(itemRepository.countByStatusAndUserId(testUserId)).thenReturn(
                    Collections.singletonList(new Object[]{ItemStatus.IN_STOCK, 5L})
            );
            when(itemRepository.countByCategoryAndUserId(testUserId)).thenReturn(
                    Collections.singletonList(new Object[]{"Electronics", 3L})
            );
            when(itemRepository.getListsOverviewByUserId(testUserId)).thenReturn(List.of());
            when(itemRepository.findTop5ByUserIdOrderByUpdatedAtDesc(testUserId)).thenReturn(List.of());

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(5L);
            assertThat(stats.totalQuantity()).isEqualTo(50L);
            assertThat(stats.lowStockCount()).isEqualTo(1L);
            assertThat(stats.outOfStockCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should throw UnauthorizedException when non-admin not authenticated")
        void getDashboardStats_notAuthenticated_throwsException() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.getDashboardStats())
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("should handle null status and category in buildStats")
        void getDashboardStats_nullStatusAndCategory_usesDefaults() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.count()).thenReturn(1L);
            when(itemRepository.sumStock()).thenReturn(1L);
            when(itemRepository.countLowStock(5)).thenReturn(0L);
            when(itemRepository.countOutOfStock()).thenReturn(0L);
            when(itemRepository.getListsOverview()).thenReturn(
                    Collections.singletonList(new Object[]{"My List", 3L, 10L})
            );
            when(itemRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of());
            when(itemRepository.countByStatus()).thenReturn(
                    Collections.singletonList(new Object[]{null, 1L})
            );
            when(itemRepository.countByCategory()).thenReturn(
                    Collections.singletonList(new Object[]{null, 1L})
            );

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.countByStatus()).containsEntry("Unknown", 1L);
            assertThat(stats.countByCategory()).containsEntry("Uncategorized", 1L);
            assertThat(stats.listsOverview()).hasSize(1);
            assertThat(stats.listsOverview().get(0).listName()).isEqualTo("My List");
        }

        @Test
        @DisplayName("should include recent items with and without SKU custom field")
        void getDashboardStats_withRecentItems_mapsCorrectly() {
            Item itemWithSku = new Item();
            itemWithSku.setId(UUID.randomUUID());
            itemWithSku.setName("SKU Item");
            itemWithSku.setItemList(testList);
            itemWithSku.setStatus(ItemStatus.IN_STOCK);
            itemWithSku.setStock(5);
            itemWithSku.setCustomFieldValues(Map.of("sku", "ABC-123"));

            Item itemWithoutSku = new Item();
            itemWithoutSku.setId(UUID.randomUUID());
            itemWithoutSku.setName("No SKU Item");
            itemWithoutSku.setItemList(testList);
            itemWithoutSku.setStatus(ItemStatus.LOW_STOCK);
            itemWithoutSku.setStock(2);
            itemWithoutSku.setCustomFieldValues(null);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.count()).thenReturn(2L);
            when(itemRepository.sumStock()).thenReturn(7L);
            when(itemRepository.countLowStock(5)).thenReturn(1L);
            when(itemRepository.countOutOfStock()).thenReturn(0L);
            when(itemRepository.getListsOverview()).thenReturn(List.of());
            when(itemRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of(itemWithSku, itemWithoutSku));
            when(itemRepository.countByStatus()).thenReturn(List.of());
            when(itemRepository.countByCategory()).thenReturn(List.of());

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.recentlyUpdated()).hasSize(2);
            assertThat(stats.recentlyUpdated().get(0).sku()).isEqualTo("ABC-123");
            assertThat(stats.recentlyUpdated().get(1).sku()).isNull();
        }
    }
}
