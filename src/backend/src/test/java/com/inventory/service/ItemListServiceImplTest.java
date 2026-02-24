package com.inventory.service;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.service.impl.ItemListServiceImpl;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemListServiceImpl Tests")
class ItemListServiceImplTest {

    @Mock
    private ItemListRepository itemListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private CustomFieldValidator customFieldValidator;

    @InjectMocks
    private ItemListServiceImpl itemListService;

    private User testUser;
    private ItemList testList;
    private UUID testUserId;
    private UUID testListId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testListId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");

        testList = new ItemList();
        testList.setId(testListId);
        testList.setName("Test List");
        testList.setCategory("Electronics");
        testList.setUser(testUser);
    }

    @Nested
    @DisplayName("getAllLists")
    class GetAllListsTests {

        @Test
        @DisplayName("admin should see all lists")
        void admin_seesAllLists() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.findAll(pageable)).thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(itemListRepository).findAll(pageable);
            verify(itemListRepository, never()).findByUserId(any(), any());
        }

        @Test
        @DisplayName("regular user should see only their lists")
        void user_seesOwnLists() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<ItemList> expected = new PageImpl<>(List.of(testList));
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findByUserId(testUserId, pageable)).thenReturn(expected);

            Page<ItemList> result = itemListService.getAllLists(pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(itemListRepository).findByUserId(testUserId, pageable);
        }

        @Test
        @DisplayName("unauthenticated user should get UnauthorizedException")
        void unauthenticated_throwsException() {
            Pageable pageable = PageRequest.of(0, 10);
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.getAllLists(pageable))
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
        @DisplayName("user should access own list")
        void user_accessesOwnList() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.findByIdAndUserId(testListId, testUserId))
                    .thenReturn(Optional.of(testList));

            ItemList result = itemListService.getListById(testListId);

            assertThat(result.getId()).isEqualTo(testListId);
        }

        @Test
        @DisplayName("user should get 404 for another user's list")
        void user_cannotAccessOthersList() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(otherUserId));
            when(itemListRepository.findByIdAndUserId(testListId, otherUserId))
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
        @DisplayName("should create list for current user")
        void createList_success() {
            ItemListRequest request = new ItemListRequest("New List", "Description", "Category", null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
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
            ItemListRequest request = new ItemListRequest("New List", null, null, null);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> itemListService.createList(request))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("deleteList")
    class DeleteListTests {

        @Test
        @DisplayName("admin should delete any list")
        void admin_deletesAnyList() {
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.existsById(testListId)).thenReturn(true);

            itemListService.deleteList(testListId);

            verify(itemListRepository).deleteById(testListId);
        }

        @Test
        @DisplayName("user should delete own list")
        void user_deletesOwnList() {
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(testUserId));
            when(itemListRepository.existsByIdAndUserId(testListId, testUserId)).thenReturn(true);

            itemListService.deleteList(testListId);

            verify(itemListRepository).deleteById(testListId);
        }

        @Test
        @DisplayName("user cannot delete another user's list")
        void user_cannotDeleteOthersList() {
            UUID otherUserId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(false);
            when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(otherUserId));
            when(itemListRepository.existsByIdAndUserId(testListId, otherUserId)).thenReturn(false);

            assertThatThrownBy(() -> itemListService.deleteList(testListId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when list does not exist")
        void nonExistingList_throwsException() {
            UUID nonExistingId = UUID.randomUUID();
            when(securityUtils.isAdmin()).thenReturn(true);
            when(itemListRepository.existsById(nonExistingId)).thenReturn(false);

            assertThatThrownBy(() -> itemListService.deleteList(nonExistingId))
                    .isInstanceOf(ItemListNotFoundException.class);
        }
    }
}
