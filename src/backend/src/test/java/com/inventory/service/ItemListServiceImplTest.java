package com.inventory.service;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.enums.CustomFieldType;
import com.inventory.enums.ItemStatus;
import com.inventory.enums.Role;
import com.inventory.enums.WorkspaceRole;
import com.inventory.exception.ExportLimitExceededException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ListLimitExceededException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.exception.WorkspaceAccessDeniedException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.UserRepository;
import com.inventory.repository.WorkspaceRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.security.WorkspaceAccessUtils;
import com.inventory.enums.ActivityEventType;
import com.inventory.service.IActivityService;
import com.inventory.service.impl.ItemListServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemListServiceImpl Tests")
class ItemListServiceImplTest {

    @Mock
    private ItemListRepository itemListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private WorkspaceAccessUtils workspaceAccessUtils;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CustomFieldValidator customFieldValidator;

    @Mock
    private IActivityService activityService;

    private ItemListServiceImpl itemListService;

    private User testUser;
    private ItemList testList;
    private Workspace testWorkspace;
    private UUID testUserId;
    private UUID testListId;
    private UUID testWorkspaceId;

    @BeforeEach
    void setUp() {
        itemListService = new ItemListServiceImpl(
                itemListRepository, itemRepository, userRepository,
                workspaceRepository, securityUtils, workspaceAccessUtils,
                customFieldValidator, true, activityService);

        testUserId = UUID.randomUUID();
        testListId = UUID.randomUUID();
        testWorkspaceId = UUID.randomUUID();

        testUser = new User();
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
    }

    @Nested
    @DisplayName("getAllLists")
    class GetAllListsTests {

        @Test
        @DisplayName("admin should see all lists")
        @SuppressWarnings("unchecked")
        void admin_seesAllLists() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable, null, null);

            assertThat(result.getContent()).hasSize(1);
            verify(itemListRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("regular user should see only their lists when no workspaceId provided")
        @SuppressWarnings("unchecked")
        void user_seesOwnLists() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable, null, null);

            assertThat(result.getContent()).hasSize(1);
            verify(itemListRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("user with no workspaces should fall back to ownerId-based query")
        @SuppressWarnings("unchecked")
        void user_noWorkspaces_fallsBackToOwnerId() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of());
            when(itemListRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable, null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("user should see filtered lists when search is provided")
        @SuppressWarnings("unchecked")
        void user_withSearch_filtersResults() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                    .thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable, null, "electronics");

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("unauthenticated user should get UnauthorizedException")
        void unauthenticated_throwsException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.getAllLists(pageable, null, null))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("getListById")
    class GetListByIdTests {

        @Test
        @DisplayName("admin should access any list")
        void admin_accessesAnyList() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));

            ItemList result = itemListService.getListById(testListId);

            assertThat(result.getId()).isEqualTo(testListId);
        }

        @Test
        @DisplayName("user should access list in accessible workspace")
        void user_accessesOwnList() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testList));

            ItemList result = itemListService.getListById(testListId);

            assertThat(result.getId()).isEqualTo(testListId);
        }

        @Test
        @DisplayName("user should get 404 for list in inaccessible workspace")
        void user_cannotAccessOthersList() {
            UUID otherWorkspaceId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(otherWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(otherWorkspaceId)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.getListById(testListId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when list does not exist")
        void nonExistingList_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.getListById(nonExistingId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("createList")
    class CreateListTests {

        @Test
        @DisplayName("should create list for current user using default workspace")
        void createList_success() {
            ItemListRequest request = new ItemListRequest("New List", "Description", "Category", null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(invocation -> {
                ItemList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.createList(request);

            assertThat(result.getName()).isEqualTo("New List");
            assertThat(result.getUser()).isEqualTo(testUser);
            verify(customFieldValidator).validateDefinitionNames(null);
            verify(itemListRepository).save(any(ItemList.class));
        }

        @Test
        @DisplayName("unauthenticated user cannot create list")
        void unauthenticated_throwsException() {
            ItemListRequest request = new ItemListRequest("New List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.createList(request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("free user should be blocked at 5 lists")
        void freeUser_blockedAtLimit() {
            testUser.setRole(Role.USER);
            ItemListRequest request = new ItemListRequest("Sixth List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.countByUserId(testUserId)).thenReturn(5L);

            assertThatThrownBy(() -> itemListService.createList(request))
                    .isInstanceOf(ListLimitExceededException.class)
                    .hasMessageContaining("5 lists");
        }

        @Test
        @DisplayName("free user should be allowed under 5 lists")
        void freeUser_allowedUnderLimit() {
            testUser.setRole(Role.USER);
            ItemListRequest request = new ItemListRequest("Fourth List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.countByUserId(testUserId)).thenReturn(3L);
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(invocation -> {
                ItemList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.createList(request);

            assertThat(result.getName()).isEqualTo("Fourth List");
            verify(itemListRepository).save(any(ItemList.class));
        }

        @Test
        @DisplayName("free user should bypass list limit when premium is disabled")
        void freeUser_noLimitWhenPremiumDisabled() {
            ItemListServiceImpl serviceWithPremiumDisabled = new ItemListServiceImpl(
                    itemListRepository, itemRepository, userRepository,
                    workspaceRepository, securityUtils, workspaceAccessUtils,
                    customFieldValidator, false, activityService);

            testUser.setRole(Role.USER);
            ItemListRequest request = new ItemListRequest("Sixth List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(invocation -> {
                ItemList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = serviceWithPremiumDisabled.createList(request);

            assertThat(result.getName()).isEqualTo("Sixth List");
            verify(itemListRepository, never()).countByUserId(any());
        }

        @Test
        @DisplayName("premium user should have no list limit")
        void premiumUser_noLimit() {
            testUser.setRole(Role.PREMIUM_USER);
            ItemListRequest request = new ItemListRequest("Unlimited List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(invocation -> {
                ItemList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.createList(request);

            assertThat(result.getName()).isEqualTo("Unlimited List");
            verify(itemListRepository, never()).countByUserId(any());
        }

        @Test
        @DisplayName("admin should have no list limit")
        void admin_noLimit() {
            testUser.setRole(Role.ADMIN);
            ItemListRequest request = new ItemListRequest("Admin List", null, null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByOwnerIdAndIsDefaultTrue(testUserId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(invocation -> {
                ItemList saved = invocation.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.createList(request);

            assertThat(result.getName()).isEqualTo("Admin List");
            verify(itemListRepository, never()).countByUserId(any());
        }
    }

    @Nested
    @DisplayName("duplicateList")
    class DuplicateListTests {

        private Item sourceItem;

        @BeforeEach
        void setUpItems() {
            sourceItem = new Item();
            sourceItem.setName("Widget");
            sourceItem.setStatus(ItemStatus.AVAILABLE);
            sourceItem.setStock(3);
            sourceItem.setBarcode("123456");
            sourceItem.setPosition(0);
            sourceItem.setCustomFieldValues(Map.of("color", "red"));
            sourceItem.setItemList(testList);
        }

        private void stubGetListById() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testList));
        }

        @Test
        @DisplayName("should clone list with '(Copie)' suffix and copy all items")
        void duplicate_success_clonesListAndItems() {
            testUser.setRole(Role.PREMIUM_USER);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(editorMember);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemRepository.findAllByItemListIdOrderByPositionAsc(testListId))
                    .thenReturn(List.of(sourceItem));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(inv -> {
                ItemList saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.duplicateList(testListId);

            assertThat(result.getName()).isEqualTo("Test List (Copie)");
            assertThat(result.getCategory()).isEqualTo("Electronics");
            assertThat(result.getWorkspace()).isEqualTo(testWorkspace);
            verify(itemRepository).saveAll(any());
        }

        @Test
        @DisplayName("VIEWER role should be denied")
        void duplicate_viewer_throwsWorkspaceAccessDeniedException() {
            WorkspaceMember viewerMember = new WorkspaceMember();
            viewerMember.setRole(WorkspaceRole.VIEWER);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(viewerMember);

            assertThatThrownBy(() -> itemListService.duplicateList(testListId))
                    .isInstanceOf(WorkspaceAccessDeniedException.class);
        }

        @Test
        @DisplayName("free user at limit should be blocked")
        void duplicate_freeUserAtLimit_throwsListLimitExceededException() {
            testUser.setRole(Role.USER);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(editorMember);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.countByUserId(testUserId)).thenReturn(5L);

            assertThatThrownBy(() -> itemListService.duplicateList(testListId))
                    .isInstanceOf(ListLimitExceededException.class)
                    .hasMessageContaining("5 lists");
        }

        @Test
        @DisplayName("free user under limit should succeed")
        void duplicate_freeUserUnderLimit_succeeds() {
            testUser.setRole(Role.USER);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(editorMember);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemListRepository.countByUserId(testUserId)).thenReturn(3L);
            when(itemRepository.findAllByItemListIdOrderByPositionAsc(testListId)).thenReturn(List.of());
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(inv -> {
                ItemList saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.duplicateList(testListId);

            assertThat(result.getName()).isEqualTo("Test List (Copie)");
        }

        @Test
        @DisplayName("admin should bypass role and limit checks")
        void duplicate_admin_bypassesChecks() {
            testUser.setRole(Role.ADMIN);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemRepository.findAllByItemListIdOrderByPositionAsc(testListId)).thenReturn(List.of());
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(inv -> {
                ItemList saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.duplicateList(testListId);

            assertThat(result.getName()).isEqualTo("Test List (Copie)");
            verify(workspaceAccessUtils, never()).requireMembership(any());
            verify(itemListRepository, never()).countByUserId(any());
        }

        @Test
        @DisplayName("source list not found should throw ItemListNotFoundException")
        void duplicate_listNotFound_throwsNotFoundException() {
            UUID nonExistingId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.duplicateList(nonExistingId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("item fields should be copied correctly — images skipped")
        void duplicate_itemFieldsCopiedCorrectly() {
            testUser.setRole(Role.PREMIUM_USER);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(editorMember);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemRepository.findAllByItemListIdOrderByPositionAsc(testListId))
                    .thenReturn(List.of(sourceItem));
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(inv -> {
                ItemList saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            itemListService.duplicateList(testListId);

            verify(itemRepository).saveAll(argThat(items -> {
                List<Item> itemList = new java.util.ArrayList<>();
                items.forEach(itemList::add);
                assertThat(itemList).hasSize(1);
                Item copy = itemList.get(0);
                assertThat(copy.getName()).isEqualTo("Widget");
                assertThat(copy.getStatus()).isEqualTo(ItemStatus.AVAILABLE);
                assertThat(copy.getStock()).isEqualTo(3);
                assertThat(copy.getBarcode()).isEqualTo("123456");
                assertThat(copy.getPosition()).isEqualTo(0);
                assertThat(copy.getCustomFieldValues()).containsEntry("color", "red");
                assertThat(copy.getImageKey()).isNull();
                return true;
            }));
        }

        @Test
        @DisplayName("list with no items should produce an empty clone")
        void duplicate_emptyList_producesEmptyClone() {
            testUser.setRole(Role.PREMIUM_USER);

            WorkspaceMember editorMember = new WorkspaceMember();
            editorMember.setRole(WorkspaceRole.EDITOR);

            stubGetListById();
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(editorMember);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            when(itemRepository.findAllByItemListIdOrderByPositionAsc(testListId)).thenReturn(List.of());
            when(itemListRepository.save(any(ItemList.class))).thenAnswer(inv -> {
                ItemList saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ItemList result = itemListService.duplicateList(testListId);

            assertThat(result.getName()).isEqualTo("Test List (Copie)");
            verify(itemRepository).saveAll(argThat(items -> {
                List<Item> itemList = new java.util.ArrayList<>();
                items.forEach(itemList::add);
                assertThat(itemList).isEmpty();
                return true;
            }));
        }
    }

    @Nested
    @DisplayName("deleteList")
    class DeleteListTests {

        @Test
        @DisplayName("admin should delete any list")
        void admin_deletesAnyList() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));

            itemListService.deleteList(testListId);

            verify(itemListRepository).delete(testList);
        }

        @Test
        @DisplayName("user (OWNER) should delete their list")
        void user_deletesOwnList() {
            WorkspaceMember ownerMember = new WorkspaceMember();
            ownerMember.setRole(WorkspaceRole.OWNER);

            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testList));
            when(workspaceAccessUtils.requireRole(testWorkspaceId, WorkspaceRole.OWNER))
                    .thenReturn(ownerMember);

            itemListService.deleteList(testListId);

            verify(itemListRepository).delete(testList);
        }

        @Test
        @DisplayName("user cannot delete list in inaccessible workspace")
        void user_cannotDeleteOthersList() {
            UUID otherWorkspaceId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(otherWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(otherWorkspaceId)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.deleteList(testListId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when list does not exist")
        void nonExistingList_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.deleteList(nonExistingId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("exportListAsCsv")
    class ExportListAsCsvTests {

        @Test
        @DisplayName("empty list should produce headers only with BOM")
        void emptyList_headersOnlyWithBom() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(0L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of());

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            assertThat(csv).startsWith("\uFEFFsep=,");
            assertThat(csv).contains("Name,Status,Stock");
            // Only sep hint + header + BOM, no data rows
            String[] lines = csv.replace("\uFEFF", "").split("\r\n");
            assertThat(lines[0]).isEqualTo("sep=,");
            assertThat(lines[1]).isEqualTo("Name,Status,Stock");
        }

        @Test
        @DisplayName("should include custom field columns in header sorted by displayOrder")
        void customFields_sortedInHeader() {
            List<CustomFieldDefinition> definitions = List.of(
                    new CustomFieldDefinition("color", "Color", CustomFieldType.TEXT, false, 2),
                    new CustomFieldDefinition("price", "Price", CustomFieldType.NUMBER, false, 1)
            );
            testList.setCustomFieldDefinitions(definitions);

            Item item = new Item();
            item.setName("Widget");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(5);
            item.setCustomFieldValues(Map.of("price", 9.99, "color", "Red"));

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(1L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of(item));

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            String[] lines = csv.replace("\uFEFF", "").split("\r\n");
            // Price (order 1) before Color (order 2)
            assertThat(lines[0]).isEqualTo("sep=,");
            assertThat(lines[1]).isEqualTo("Name,Status,Stock,Price,Color");
            assertThat(lines[2]).isEqualTo("Widget,AVAILABLE,5,9.99,Red");
        }

        @Test
        @DisplayName("should escape fields with commas and quotes")
        void csvEscaping_commasAndQuotes() {
            Item item = new Item();
            item.setName("Widget, \"Deluxe\"");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(3);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(1L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of(item));

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            String[] lines = csv.replace("\uFEFF", "").split("\r\n");
            // Commas and quotes in name should be escaped
            assertThat(lines[2]).startsWith("\"Widget, \"\"Deluxe\"\"\"");
        }

        @Test
        @DisplayName("should protect against CSV injection")
        void csvInjection_prefixedWithTab() {
            Item item = new Item();
            item.setName("=HYPERLINK(\"evil\")");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(1);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(1L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of(item));

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            // Field starting with = should be prefixed with tab and always quoted
            assertThat(csv).contains("\"\t=HYPERLINK(\"\"evil\"\")\"");
        }

        @Test
        @DisplayName("should handle null custom field values")
        void nullCustomFieldValues_emptyCell() {
            List<CustomFieldDefinition> definitions = List.of(
                    new CustomFieldDefinition("notes", "Notes", CustomFieldType.TEXT, false, 1)
            );
            testList.setCustomFieldDefinitions(definitions);

            Item item = new Item();
            item.setName("Widget");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(2);
            item.setCustomFieldValues(null);

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(1L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of(item));

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            String[] lines = csv.replace("\uFEFF", "").split("\r\n");
            // Data row should end with comma (empty custom field)
            assertThat(lines[2]).isEqualTo("Widget,AVAILABLE,2,");
        }

        @Test
        @DisplayName("filename should be sanitized and contain date")
        void filename_sanitizedWithDate() {
            testList.setName("Café / Résumé!");

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(0L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of());

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            // Spaces and special chars replaced with _, consecutive _ collapsed, leading/trailing _ trimmed
            assertThat(result.filename()).matches("Caf_R_sum_.*\\.csv");
            assertThat(result.filename()).endsWith(".csv");
        }

        @Test
        @DisplayName("should throw when export exceeds item limit")
        void exportExceedsLimit_throwsException() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(10_001L);

            assertThatThrownBy(() -> itemListService.exportListAsCsv(testListId))
                    .isInstanceOf(ExportLimitExceededException.class)
                    .hasMessageContaining("10000");

            // Verify items are never loaded when count exceeds limit
            verify(itemRepository, never()).findAllByItemListIdOrderByCreatedAtAsc(any());
        }

        @Test
        @DisplayName("should preserve negative numbers without injection prefix")
        void negativeNumbers_notPrefixed() {
            List<CustomFieldDefinition> definitions = List.of(
                    new CustomFieldDefinition("temp", "Temperature", CustomFieldType.NUMBER, false, 1)
            );
            testList.setCustomFieldDefinitions(definitions);

            Item item = new Item();
            item.setName("Freezer");
            item.setStatus(ItemStatus.AVAILABLE);
            item.setStock(1);
            item.setCustomFieldValues(Map.of("temp", -5));

            when(securityUtils.isAdmin()).thenReturn(true);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findById(testListId)).thenReturn(Optional.of(testList));
            when(itemRepository.countByItemListId(testListId)).thenReturn(1L);
            when(itemRepository.findAllByItemListIdOrderByCreatedAtAsc(testListId)).thenReturn(List.of(item));

            CsvExportResult result = itemListService.exportListAsCsv(testListId);

            String csv = new String(result.content(), StandardCharsets.UTF_8);
            String[] lines = csv.replace("\uFEFF", "").split("\r\n");
            // -5 is a legitimate number, should NOT be tab-prefixed
            assertThat(lines[2]).isEqualTo("Freezer,AVAILABLE,1,-5");
        }

        @Test
        @DisplayName("should throw when list not found")
        void listNotFound_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findById(nonExistingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.exportListAsCsv(nonExistingId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("activity recording")
    class ActivityRecordingTests {

        @Test
        @DisplayName("createList records LIST_CREATED event")
        void createList_recordsCreatedEvent() {
            ItemListRequest request = new ItemListRequest("My List", null, null, null, testWorkspaceId);

            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findByIdWithLock(testUserId)).thenReturn(Optional.of(testUser));
            when(workspaceRepository.findByIdWithLock(testWorkspaceId)).thenReturn(Optional.of(testWorkspace));
            WorkspaceMember member = new WorkspaceMember();
            member.setRole(WorkspaceRole.EDITOR);
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(member);
            when(itemListRepository.save(any())).thenReturn(testList);

            itemListService.createList(request);

            verify(activityService).record(testWorkspaceId, ActivityEventType.LIST_CREATED,
                    "LIST", testList.getId(), testList.getName());
        }

        @Test
        @DisplayName("updateList records LIST_UPDATED event")
        void updateList_recordsUpdatedEvent() {
            ItemListRequest request = new ItemListRequest("Updated List", null, null, null, testWorkspaceId);

            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testList));
            WorkspaceMember member = new WorkspaceMember();
            member.setRole(WorkspaceRole.EDITOR);
            when(workspaceAccessUtils.requireMembership(testWorkspaceId)).thenReturn(member);
            when(itemListRepository.save(any())).thenReturn(testList);

            itemListService.updateList(testListId, request);

            verify(activityService).record(testWorkspaceId, ActivityEventType.LIST_UPDATED,
                    "LIST", testList.getId(), testList.getName());
        }

        @Test
        @DisplayName("deleteList records LIST_DELETED event after deletion")
        void deleteList_recordsDeletedEvent() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(workspaceAccessUtils.getAccessibleWorkspaceIds()).thenReturn(List.of(testWorkspaceId));
            when(itemListRepository.findByIdAndWorkspaceIdIn(testListId, List.of(testWorkspaceId)))
                    .thenReturn(Optional.of(testList));
            when(workspaceAccessUtils.requireRole(testWorkspaceId, WorkspaceRole.OWNER)).thenReturn(null);

            itemListService.deleteList(testListId);

            verify(activityService).record(testWorkspaceId, ActivityEventType.LIST_DELETED,
                    "LIST", testListId, "Test List");
        }
    }
}
