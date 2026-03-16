package com.inventory.service.impl;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.enums.Role;
import com.inventory.exception.ExportLimitExceededException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ListLimitExceededException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IItemListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemListServiceImpl implements IItemListService {

    private static final int MAX_EXPORT_ITEMS = 10_000;

    private final ItemListRepository itemListRepository;
    private final ItemRepository itemRepository;
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

    @Override
    @Transactional(readOnly = true)
    public CsvExportResult exportListAsCsv(@NonNull UUID id) {
        ItemList itemList = getListById(id);

        long itemCount = itemRepository.countByItemListId(id);
        if (itemCount > MAX_EXPORT_ITEMS) {
            throw new ExportLimitExceededException(
                    "Cannot export more than " + MAX_EXPORT_ITEMS + " items. Please reduce the list size.");
        }

        List<Item> items = itemRepository.findAllByItemListIdOrderByCreatedAtAsc(id);

        log.info("User {} exporting list {} ({} items)",
                securityUtils.getCurrentUserId().orElse(null), id, items.size());

        List<CustomFieldDefinition> customFields = itemList.getCustomFieldDefinitions();
        if (customFields == null) {
            customFields = List.of();
        }
        List<CustomFieldDefinition> sortedFields = customFields.stream()
                .sorted(Comparator.comparingInt(CustomFieldDefinition::displayOrder))
                .toList();

        StringBuilder csv = new StringBuilder(items.size() * 128);
        csv.append('\uFEFF'); // UTF-8 BOM for Excel compatibility
        csv.append("sep=,\r\n"); // Tell Excel to use comma as delimiter (fixes European locales)

        // Header row
        csv.append("Name,Status,Stock");
        for (CustomFieldDefinition field : sortedFields) {
            csv.append(',').append(escapeCsvField(field.label()));
        }
        csv.append("\r\n");

        // Data rows
        for (Item item : items) {
            csv.append(escapeCsvField(item.getName()));
            csv.append(',').append(escapeCsvField(item.getStatus().name()));
            csv.append(',').append(item.getStock() != null ? item.getStock() : 0);

            Map<String, Object> values = item.getCustomFieldValues();
            for (CustomFieldDefinition field : sortedFields) {
                csv.append(',');
                if (values != null && values.containsKey(field.name())) {
                    Object value = values.get(field.name());
                    csv.append(escapeCsvField(formatFieldValue(value)));
                }
            }
            csv.append("\r\n");
        }

        String sanitizedName = itemList.getName()
                .replaceAll("[^a-zA-Z0-9\\-_]", "_")
                .replaceAll("_{2,}", "_")
                .replaceAll("^_|_$", "");
        if (sanitizedName.isEmpty() || sanitizedName.length() > 100) {
            sanitizedName = sanitizedName.isEmpty() ? "export" : sanitizedName.substring(0, 100);
        }
        String filename = sanitizedName + "_" + LocalDate.now() + ".csv";

        return new CsvExportResult(csv.toString().getBytes(StandardCharsets.UTF_8), filename);
    }

    private String formatFieldValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        log.warn("Unexpected custom field value type: {}", value.getClass().getName());
        return value.toString();
    }

    private String escapeCsvField(String field) {
        if (field == null || field.isEmpty()) {
            return "";
        }

        // CSV injection protection: prefix formula-triggering characters with a tab
        // Skip legitimate numeric values (e.g., -5, +3.14)
        char first = field.charAt(0);
        boolean injectionPrefixed = false;
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '|') {
            if (!field.matches("[+\\-]?\\d+(\\.\\d+)?")) {
                field = "\t" + field;
                injectionPrefixed = true;
            }
        }

        // Wrap in quotes if the field contains special characters or was injection-prefixed
        if (injectionPrefixed || field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
