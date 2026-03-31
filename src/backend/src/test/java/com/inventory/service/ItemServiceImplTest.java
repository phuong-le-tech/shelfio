package com.inventory.service;

import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.response.DashboardStats;
import com.inventory.enums.ActivityEventType;
import com.inventory.enums.ItemStatus;
import com.inventory.enums.WorkspaceRole;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.service.IActivityService;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.security.WorkspaceAccessUtils;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private WorkspaceAccessUtils workspaceAccessUtils;

    @Mock
    private ImageStorageService imageStorageService;

    @Mock
    private ImageProcessingService imageProcessingService;

    @Mock
    private IActivityService activityService;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Item testItem;
    private ItemList testList;
    private Workspace testWorkspace;
    private UUID testId;
    private UUID testListId;
    private UUID testUserId;
    private UUID testWorkspaceId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testListId = UUID.randomUUID();
        testUserId = UUID.randomUUID();
        testWorkspaceId = UUID.randomUUID();

        User testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");

        testWorkspace = new Workspace();
        testWorkspace.setId(testWorkspaceId);
        testWorkspace.setName("Test Workspace");

        testList = new ItemList();
        testList.setId(testListId);
        testList.setName("Test List");
        testList.setCategory("Electronics");
        testList.setUser(testUser);
        testList.setWorkspace(testWorkspace);

        testItem = new Item();
        testItem.setId(testId);
        testItem.setName("Test Item");
        testItem.setItemList(testList);
        testItem.setStatus(ItemStatus.AVAILABLE);
        testItem.setStock(10);
    }

    @Nested
    @DisplayName("getAllItems")
    class GetAllItemsTests {

        @Test
        @DisplayName("should return page of items")
        void getAllItems_returnsPageOfItems() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null, null);
            Page<Item> expectedPage = new PageImpl<>(Objects.requireNonNull(List.of(testItem), "List of items not found"));

            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
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
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null, null);
            Page<Item> expectedPage = new PageImpl<>(List.of(testItem));

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(expectedPage);

            Page<Item> result = itemService.getAllItems(pageable, criteria);

            assertThat(result.getContent()).hasSize(1);
            verify(securityUtils, never()).getCurrentUserId();
        }

        @Test
        @DisplayName("should return empty page when workspace list is empty for non-admin")
        void getAllItems_notAuthenticated_returnsEmpty() {
            Pageable pageable = PageRequest.of(0, 10);
            ItemSearchCriteria criteria = new ItemSearchCriteria(null, null, null, null);
            Page<Item> emptyPage = new PageImpl<>(List.of());

            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of());
            when(itemRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

            Page<Item> result = itemService.getAllItems(pageable, criteria);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getItemById")
    class GetItemByIdTests {

        @Test
        @DisplayName("should return item when exists and user has workspace access")
        void getItemById_existingId_returnsItem() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));

            Optional<Item> result = itemService.getItemById(testId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(testId);
            assertThat(result.get().getName()).isEqualTo("Test Item");
        }

        @Test
        @DisplayName("should return empty when item not found")
        void getItemById_nonExistingId_returnsEmpty() {
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
        @DisplayName("should throw when user has no access to item's workspace")
        void getItemById_nonOwner_throwsException() {
            UUID otherWorkspaceId = UUID.randomUUID();
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(otherWorkspaceId));

            assertThatThrownBy(() -> itemService.getItemById(testId))
                    .isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getItemByBarcode")
    class GetItemByBarcodeTests {

        @Test
        @DisplayName("should return item when barcode matches in accessible workspace")
        void getItemByBarcode_found_returnsItem() {
            String barcode = "1234567890";
            testItem.setBarcode(barcode);
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemRepository.findByBarcodeAndWorkspaceIds(barcode, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testItem));

            Optional<Item> result = itemService.getItemByBarcode(barcode);

            assertThat(result).isPresent();
            assertThat(result.get().getBarcode()).isEqualTo(barcode);
            verify(itemRepository).findByBarcodeAndWorkspaceIds(barcode, List.of(testWorkspaceId));
        }

        @Test
        @DisplayName("should return empty when barcode not found")
        void getItemByBarcode_notFound_returnsEmpty() {
            String barcode = "NONEXISTENT";
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemRepository.findByBarcodeAndWorkspaceIds(barcode, List.of(testWorkspaceId)))
                    .thenReturn(Optional.empty());

            Optional<Item> result = itemService.getItemByBarcode(barcode);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when workspace list is empty")
        void getItemByBarcode_noWorkspaces_returnsEmpty() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of());

            Optional<Item> result = itemService.getItemByBarcode("1234567890");

            assertThat(result).isEmpty();
            verify(itemRepository, never()).findByBarcodeAndWorkspaceIds(any(), any());
        }
    }

    @Nested
    @DisplayName("createItem")
    class CreateItemTests {

        private WorkspaceMember ownerMember;

        @BeforeEach
        void setUpOwnerMember() {
            ownerMember = new WorkspaceMember();
            ownerMember.setRole(WorkspaceRole.OWNER);
        }

        @Test
        @DisplayName("should create item with valid request")
        void createItem_validRequest_createsItem() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.AVAILABLE, 5, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            Item result = itemService.createItem(request, null);

            assertThat(result.getName()).isEqualTo("New Item");
            assertThat(result.getItemList()).isEqualTo(testList);
            assertThat(result.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
            assertThat(result.getStock()).isEqualTo(5);
            verify(itemRepository).save(any(Item.class));
        }

        @Test
        @DisplayName("should create item with default status when not provided")
        void createItem_noStatus_usesDefaultStatus() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, null, null, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
            assertThat(result.getStock()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception when list not found")
        void createItem_listNotFound_throwsException() {
            UUID nonExistingListId = UUID.randomUUID();
            ItemRequest request = new ItemRequest("New Item", nonExistingListId, ItemStatus.AVAILABLE, 5, null, null);
            when(itemListRepository.findByIdWithLock(nonExistingListId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.createItem(request, null))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should upload image to S3 when provided")
        void createItem_withImage_uploadsToS3() throws IOException {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.DAMAGED, 10, null, null);
            // JPEG magic bytes (FF D8 FF) followed by dummy data
            byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile(
                    "image", "test.jpg", "image/jpeg", jpegBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(imageProcessingService.processToWebP(any())).thenReturn(new byte[]{1, 2, 3});
            when(imageStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn("items/test-key.webp");

            Item result = itemService.createItem(request, image);

            assertThat(result.getImageKey()).isNotNull();
            verify(imageStorageService).upload(anyString(), any(byte[].class), eq("image/webp"));
            verify(itemRepository, times(2)).save(any(Item.class));
        }

        @Test
        @DisplayName("should create item with PNG image")
        void createItem_withPngImage_uploadsToS3() throws IOException {
            ItemRequest request = new ItemRequest("PNG Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.png", "image/png", pngBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(imageProcessingService.processToWebP(any())).thenReturn(new byte[]{1, 2, 3});
            when(imageStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn("items/test-key.webp");

            Item result = itemService.createItem(request, image);

            assertThat(result.getImageKey()).isNotNull();
        }

        @Test
        @DisplayName("should create item with GIF image")
        void createItem_withGifImage_uploadsToS3() throws IOException {
            ItemRequest request = new ItemRequest("GIF Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            byte[] gifBytes = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.gif", "image/gif", gifBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(imageProcessingService.processToWebP(any())).thenReturn(new byte[]{1, 2, 3});
            when(imageStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn("items/test-key.webp");

            Item result = itemService.createItem(request, image);

            assertThat(result.getImageKey()).isNotNull();
        }

        @Test
        @DisplayName("should create item with WebP image")
        void createItem_withWebpImage_uploadsToS3() throws IOException {
            ItemRequest request = new ItemRequest("WebP Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            // RIFF....WEBP
            byte[] webpBytes = new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50};
            MockMultipartFile image = new MockMultipartFile("image", "test.webp", "image/webp", webpBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(imageProcessingService.processToWebP(any())).thenReturn(new byte[]{1, 2, 3});
            when(imageStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn("items/test-key.webp");

            Item result = itemService.createItem(request, image);

            assertThat(result.getImageKey()).isNotNull();
        }

        @Test
        @DisplayName("should throw exception for file too large")
        void createItem_fileTooLarge_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            byte[] largeFile = new byte[10 * 1024 * 1024 + 1]; // just over 10MB
            largeFile[0] = (byte) 0xFF;
            largeFile[1] = (byte) 0xD8;
            largeFile[2] = (byte) 0xFF;
            MockMultipartFile image = new MockMultipartFile("image", "big.jpg", "image/jpeg", largeFile);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("10MB");
        }

        @Test
        @DisplayName("should throw exception for invalid file type")
        void createItem_invalidFileType_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            byte[] invalidBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0, 0, 0, 0, 0, 0, 0, 0}; // PDF magic
            MockMultipartFile image = new MockMultipartFile("image", "test.pdf", "application/pdf", invalidBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        @DisplayName("should throw exception for file too small to detect type")
        void createItem_fileTooSmall_throwsException() {
            ItemRequest request = new ItemRequest("Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            byte[] tinyBytes = new byte[]{0x01, 0x02};
            MockMultipartFile image = new MockMultipartFile("image", "tiny.bin", "application/octet-stream", tinyBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            assertThatThrownBy(() -> itemService.createItem(request, image))
                    .isInstanceOf(FileValidationException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        @DisplayName("should throw when non-member tries to create item in workspace list")
        void createItem_nonOwner_throwsException() {
            ItemRequest request = new ItemRequest("New Item", testListId, ItemStatus.AVAILABLE, 5, null, null);

            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.requireMembership(testWorkspaceId))
                    .thenThrow(new com.inventory.exception.WorkspaceNotFoundException(testWorkspaceId));

            assertThatThrownBy(() -> itemService.createItem(request, null))
                    .isInstanceOf(com.inventory.exception.WorkspaceNotFoundException.class);
        }

        @Test
        @DisplayName("should create item as workspace member (non-admin)")
        void createItem_asOwner_createsItem() throws IOException {
            ItemRequest request = new ItemRequest("Owner Item", testListId, ItemStatus.AVAILABLE, 3, null, null);

            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(ownerMember);
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.createItem(request, null);

            assertThat(result.getName()).isEqualTo("Owner Item");
        }

        @Test
        @DisplayName("should record ITEM_CREATED activity after saving item")
        void createItem_recordsActivity() throws IOException {
            ItemRequest request = new ItemRequest("Activity Item", testListId, ItemStatus.AVAILABLE, 1, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
                Item saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            itemService.createItem(request, null);

            verify(activityService).record(
                    eq(testWorkspaceId),
                    eq(ActivityEventType.ITEM_CREATED),
                    eq("ITEM"),
                    any(UUID.class),
                    eq("Activity Item"));
        }
    }

    @Nested
    @DisplayName("updateItem")
    class UpdateItemTests {

        @Test
        @DisplayName("should update existing item")
        void updateItem_existingId_updatesItem() throws IOException {
            ItemRequest request = new ItemRequest("Updated Name", testListId, ItemStatus.TO_VERIFY, 20, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getName()).isEqualTo("Updated Name");
            assertThat(result.getItemList()).isEqualTo(testList);
            assertThat(result.getStatus()).isEqualTo(ItemStatus.TO_VERIFY);
            assertThat(result.getStock()).isEqualTo(20);
        }

        @Test
        @DisplayName("should keep existing status when not provided in request")
        void updateItem_noStatus_keepsExistingStatus() throws IOException {
            testItem.setStatus(ItemStatus.DAMAGED);
            ItemRequest request = new ItemRequest("Updated Name", testListId, null, null, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Item result = itemService.updateItem(testId, request, null);

            assertThat(result.getStatus()).isEqualTo(ItemStatus.DAMAGED);
        }

        @Test
        @DisplayName("should update item with image")
        void updateItem_withImage_uploadsToS3() throws IOException {
            ItemRequest request = new ItemRequest("Updated", testListId, ItemStatus.AVAILABLE, 5, null, null);
            byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
            MockMultipartFile image = new MockMultipartFile("image", "test.png", "image/png", pngBytes);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(imageProcessingService.processToWebP(any())).thenReturn(new byte[]{1, 2, 3});
            when(imageStorageService.upload(anyString(), any(byte[].class), anyString())).thenReturn("items/test-key.webp");

            Item result = itemService.updateItem(testId, request, image);

            assertThat(result.getImageKey()).isNotNull();
            verify(imageStorageService).upload(anyString(), any(byte[].class), eq("image/webp"));
        }

        @Test
        @DisplayName("should throw when item not found")
        void updateItem_notFound_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            ItemRequest request = new ItemRequest("Updated", testListId, ItemStatus.AVAILABLE, 5, null, null);

            when(itemRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemService.updateItem(nonExistingId, request, null))
                    .isInstanceOf(ItemNotFoundException.class);
        }

        @Test
        @DisplayName("should record ITEM_UPDATED activity after saving item")
        void updateItem_recordsActivity() throws IOException {
            ItemRequest request = new ItemRequest("Updated Name", testListId, ItemStatus.AVAILABLE, 5, null, null);
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(itemListRepository.findByIdWithLock(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

            itemService.updateItem(testId, request, null);

            verify(activityService).record(
                    eq(testWorkspaceId),
                    eq(ActivityEventType.ITEM_UPDATED),
                    eq("ITEM"),
                    eq(testId),
                    eq("Updated Name"));
        }
    }

    @Nested
    @DisplayName("deleteItem")
    class DeleteItemTests {

        @Test
        @DisplayName("should delete existing item as workspace member")
        void deleteItem_existingId_deletesItem() {
            WorkspaceMember ownerMember = new WorkspaceMember();
            ownerMember.setRole(WorkspaceRole.OWNER);

            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(ownerMember);
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
        @DisplayName("should throw when non-member tries to delete item")
        void deleteItem_nonOwner_throwsException() {
            UUID otherWorkspaceId = UUID.randomUUID();
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(otherWorkspaceId));

            assertThatThrownBy(() -> itemService.deleteItem(testId))
                    .isInstanceOf(ItemNotFoundException.class);
        }

        @Test
        @DisplayName("should record ITEM_DELETED activity before deleting item")
        void deleteItem_recordsActivity() {
            when(itemRepository.findById(testId)).thenReturn(Optional.of(testItem));
            when(securityUtils.isAdmin()).thenReturn(true);
            doNothing().when(itemRepository).delete(testItem);

            itemService.deleteItem(testId);

            verify(activityService).record(
                    eq(testWorkspaceId),
                    eq(ActivityEventType.ITEM_DELETED),
                    eq("ITEM"),
                    eq(testId),
                    eq("Test Item"));
            verify(itemRepository).delete(testItem);
        }
    }

    @Nested
    @DisplayName("getDashboardStats")
    class GetDashboardStatsTests {

        @Test
        @DisplayName("should return dashboard statistics for admin")
        void getDashboardStats_returnsStats() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.count()).thenReturn(10L);
            when(itemRepository.sumStock()).thenReturn(100L);
            when(itemRepository.getListsOverview()).thenReturn(List.of());
            when(itemRepository.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of());
            when(itemRepository.countByStatus()).thenReturn(List.of(
                    new Object[]{ItemStatus.AVAILABLE, 5L},
                    new Object[]{ItemStatus.TO_VERIFY, 3L},
                    new Object[]{ItemStatus.NEEDS_MAINTENANCE, 1L},
                    new Object[]{ItemStatus.DAMAGED, 1L}
            ));
            when(itemRepository.countByCategory()).thenReturn(List.of(
                    new Object[]{"Electronics", 6L},
                    new Object[]{"Clothing", 4L}
            ));

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(10L);
            assertThat(stats.totalQuantity()).isEqualTo(100L);
            assertThat(stats.toVerifyCount()).isEqualTo(3L);
            assertThat(stats.needsAttentionCount()).isEqualTo(2L);
            assertThat(stats.countByStatus()).containsEntry("AVAILABLE", 5L);
            assertThat(stats.countByStatus()).containsEntry("TO_VERIFY", 3L);
            assertThat(stats.countByCategory()).containsEntry("Electronics", 6L);
        }

        @Test
        @DisplayName("should return workspace-scoped dashboard statistics for non-admin")
        void getDashboardStats_nonAdmin_returnsWorkspaceScopedStats() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemRepository.countByWorkspaceIds(List.of(testWorkspaceId))).thenReturn(5L);
            when(itemRepository.sumStockByWorkspaceIds(List.of(testWorkspaceId))).thenReturn(50L);
            when(itemRepository.countByStatusAndWorkspaceIds(List.of(testWorkspaceId))).thenReturn(
                    Collections.singletonList(new Object[]{ItemStatus.AVAILABLE, 5L})
            );
            when(itemRepository.countByCategoryAndWorkspaceIds(List.of(testWorkspaceId))).thenReturn(
                    Collections.singletonList(new Object[]{"Electronics", 3L})
            );
            when(itemRepository.getListsOverviewByWorkspaceIds(List.of(testWorkspaceId))).thenReturn(List.of());
            when(itemRepository.findTop5ByWorkspaceIdsOrderByUpdatedAtDesc(List.of(testWorkspaceId))).thenReturn(List.of());

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(5L);
            assertThat(stats.totalQuantity()).isEqualTo(50L);
            assertThat(stats.toVerifyCount()).isEqualTo(0L);
            assertThat(stats.needsAttentionCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return empty stats when user has no accessible workspaces")
        void getDashboardStats_noWorkspaces_returnsEmptyStats() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of());

            DashboardStats stats = itemService.getDashboardStats();

            assertThat(stats.totalItems()).isEqualTo(0L);
            assertThat(stats.totalQuantity()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should handle null status and category in buildStats")
        void getDashboardStats_nullStatusAndCategory_usesDefaults() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemRepository.count()).thenReturn(1L);
            when(itemRepository.sumStock()).thenReturn(1L);
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
    }
}
