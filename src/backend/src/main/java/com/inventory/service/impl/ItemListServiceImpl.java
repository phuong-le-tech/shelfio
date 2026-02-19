package com.inventory.service.impl;

import com.inventory.dto.request.ItemListRequest;
import com.inventory.exception.ItemListNotFoundException;
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

    @Override
    @Transactional(readOnly = true)
    public Page<ItemList> getAllLists(@NonNull Pageable pageable) {
        if (securityUtils.isAdmin()) {
            return itemListRepository.findAll(pageable);
        }
        UUID userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        return itemListRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemList getListById(@NonNull UUID id) {
        if (securityUtils.isAdmin()) {
            return itemListRepository.findByIdWithItems(id)
                    .orElseThrow(() -> new ItemListNotFoundException(id));
        }
        UUID userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        return itemListRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ItemListNotFoundException(id));
    }

    @Override
    @Transactional
    public ItemList createList(@NonNull ItemListRequest request) {
        UUID userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

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
        UUID userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        if (!itemListRepository.existsByIdAndUserId(id, userId)) {
            throw new ItemListNotFoundException(id);
        }
        itemListRepository.deleteById(id);
    }
}
