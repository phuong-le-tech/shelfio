package com.inventory.service.impl;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.enums.Role;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ListLimitExceededException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IItemListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemListServiceImpl implements IItemListService {

    private final ItemListRepository itemListRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final CustomFieldValidator customFieldValidator;

    private @NonNull UUID requireCurrentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemList> getAllLists(@NonNull Pageable pageable) {
        if (securityUtils.isAdmin()) {
            return itemListRepository.findAll(pageable);
        }
        return itemListRepository.findByUserId(requireCurrentUserId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemList getListById(@NonNull UUID id) {
        if (securityUtils.isAdmin()) {
            return itemListRepository.findById(id)
                    .orElseThrow(() -> new ItemListNotFoundException(id));
        }
        return itemListRepository.findByIdAndUserId(id, requireCurrentUserId())
                .orElseThrow(() -> new ItemListNotFoundException(id));
    }

    @Override
    @Transactional
    public ItemList createList(@NonNull ItemListRequest request) {
        UUID userId = requireCurrentUserId();

        // Use pessimistic lock to prevent race condition on concurrent list creation
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.getRole() == Role.USER) {
            long count = itemListRepository.countByUserId(userId);
            if (count >= 5) {
                throw new ListLimitExceededException(
                        "Free plan limited to 5 lists. Upgrade to Premium for unlimited lists.");
            }
        }

        customFieldValidator.validateDefinitionNames(request.customFieldDefinitions());

        ItemList itemList = new ItemList();
        itemList.setName(request.name());
        itemList.setDescription(request.description());
        itemList.setCategory(request.category());
        itemList.setCustomFieldDefinitions(request.customFieldDefinitions());
        itemList.setUser(user);
        return itemListRepository.save(itemList);
    }

    @Override
    @Transactional
    public ItemList updateList(@NonNull UUID id, @NonNull ItemListRequest request) {
        customFieldValidator.validateDefinitionNames(request.customFieldDefinitions());

        ItemList itemList = getListById(id);
        itemList.setName(request.name());
        itemList.setDescription(request.description());
        itemList.setCategory(request.category());
        itemList.setCustomFieldDefinitions(request.customFieldDefinitions());
        return itemListRepository.save(itemList);
    }

    @Override
    @Transactional
    public void deleteList(@NonNull UUID id) {
        if (securityUtils.isAdmin()) {
            if (!itemListRepository.existsById(id)) {
                throw new ItemListNotFoundException(id);
            }
            itemListRepository.deleteById(id);
            return;
        }
        if (!itemListRepository.existsByIdAndUserId(id, requireCurrentUserId())) {
            throw new ItemListNotFoundException(id);
        }
        itemListRepository.deleteById(id);
    }
}
