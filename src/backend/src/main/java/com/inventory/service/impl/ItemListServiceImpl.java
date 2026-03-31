package com.inventory.service.impl;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.dto.request.ItemListRequest;
import com.inventory.dto.response.CsvExportResult;
import com.inventory.enums.Role;
import com.inventory.enums.WorkspaceRole;
import com.inventory.exception.ExportLimitExceededException;
import com.inventory.exception.ItemListNotFoundException;
import com.inventory.exception.ListLimitExceededException;
import com.inventory.exception.UnauthorizedException;
import com.inventory.exception.WorkspaceAccessDeniedException;
import com.inventory.exception.WorkspaceNotFoundException;
import com.inventory.model.Item;
import com.inventory.model.ItemList;
import com.inventory.model.User;
import com.inventory.model.Workspace;
import com.inventory.model.WorkspaceMember;
import com.inventory.repository.ItemListRepository;
import com.inventory.repository.specification.ItemListSpecification;
import com.inventory.repository.ItemRepository;
import com.inventory.repository.UserRepository;
import com.inventory.repository.WorkspaceRepository;
import com.inventory.security.SecurityUtils;
import com.inventory.security.WorkspaceAccessUtils;
import com.inventory.service.CustomFieldValidator;
import com.inventory.service.IItemListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ItemListServiceImpl implements IItemListService {

    private static final int MAX_EXPORT_ITEMS = 10_000;

    private final ItemListRepository itemListRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceAccessUtils workspaceAccessUtils;
    private final CustomFieldValidator customFieldValidator;
    private final boolean premiumEnabled;

    public ItemListServiceImpl(ItemListRepository itemListRepository,
                               ItemRepository itemRepository,
                               UserRepository userRepository,
                               WorkspaceRepository workspaceRepository,
                               SecurityUtils securityUtils,
                               WorkspaceAccessUtils workspaceAccessUtils,
                               CustomFieldValidator customFieldValidator,
                               @Value("${app.premium.enabled:true}") boolean premiumEnabled) {
        this.itemListRepository = itemListRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.securityUtils = securityUtils;
        this.workspaceAccessUtils = workspaceAccessUtils;
        this.customFieldValidator = customFieldValidator;
        this.premiumEnabled = premiumEnabled;
    }

    private @NonNull UUID requireCurrentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ItemList> getAllLists(@NonNull Pageable pageable, UUID workspaceId, String search) {
        if (securityUtils.isAdmin()) {
            if (workspaceId != null) {
                return itemListRepository.findAll(
                        ItemListSpecification.withCriteria(search, null, null, List.of(workspaceId)),
                        pageable);
            }
            return itemListRepository.findAll(
                    ItemListSpecification.withCriteria(search, null, null, null),
                    pageable);
        }

        if (workspaceId == null) {
            // Search across all accessible workspaces
            List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
            if (!workspaceIds.isEmpty()) {
                return itemListRepository.findAll(
                        ItemListSpecification.withCriteria(search, null, null, workspaceIds),
                        pageable);
            }
            UUID userId = requireCurrentUserId();
            return itemListRepository.findAll(
                    ItemListSpecification.withCriteria(search, null, userId, null),
                    pageable);
        }

        // Verify membership
        workspaceAccessUtils.requireMembership(workspaceId);
        return itemListRepository.findAll(
                ItemListSpecification.withCriteria(search, null, null, List.of(workspaceId)),
                pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemList getListById(@NonNull UUID id) {
        if (securityUtils.isAdmin()) {
            return itemListRepository.findById(id)
                    .orElseThrow(() -> new ItemListNotFoundException(id));
        }

        // Find the list and check workspace membership
        List<UUID> workspaceIds = workspaceAccessUtils.getAccessibleWorkspaceIds();
        return itemListRepository.findByIdAndWorkspaceIdIn(id, workspaceIds)
                .orElseThrow(() -> new ItemListNotFoundException(id));
    }

    @Override
    @Transactional
    public ItemList createList(@NonNull ItemListRequest request) {
        UUID userId = requireCurrentUserId();

        // Use pessimistic lock to prevent race condition on concurrent list creation
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Resolve workspace (lock the row to serialize concurrent list creation)
        Workspace workspace;
        if (request.workspaceId() != null) {
            workspace = workspaceRepository.findByIdWithLock(request.workspaceId())
                    .orElseThrow(() -> new WorkspaceNotFoundException(request.workspaceId()));
            // Verify OWNER or EDITOR role
            WorkspaceMember member = workspaceAccessUtils.requireMembership(workspace.getId());
            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new WorkspaceAccessDeniedException("Viewers cannot create lists");
            }
        } else {
            // Default workspace — user row is already locked above, which serializes
            // creation for the user's own default workspace
            workspace = workspaceRepository.findByOwnerIdAndIsDefaultTrue(userId)
                    .orElseThrow(() -> new UnauthorizedException("No default workspace found"));
        }

        // Free-tier limit: count ALL lists owned by the user (across all workspaces)
        if (premiumEnabled && user.getRole() == Role.USER) {
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
        itemList.setWorkspace(workspace);
        return itemListRepository.save(itemList);
    }

    @Override
    @Transactional
    public ItemList updateList(@NonNull UUID id, @NonNull ItemListRequest request) {
        customFieldValidator.validateDefinitionNames(request.customFieldDefinitions());

        ItemList itemList = getListById(id);

        // Verify EDITOR or OWNER role
        if (!securityUtils.isAdmin()) {
            WorkspaceMember member = workspaceAccessUtils.requireMembership(itemList.getWorkspace().getId());
            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new WorkspaceAccessDeniedException("Viewers cannot update lists");
            }
        }

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

        ItemList itemList = getListById(id);

        // Only workspace OWNER can delete lists
        workspaceAccessUtils.requireRole(itemList.getWorkspace().getId(), WorkspaceRole.OWNER);

        itemListRepository.delete(itemList);
    }

    @Override
    @Transactional(readOnly = true)
    public CsvExportResult exportListAsCsv(@NonNull UUID id) {
        // getListById already checks workspace membership (any role can export)
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

    @Override
    @Transactional
    @SuppressWarnings("null")
    public ItemList duplicateList(@NonNull UUID id) {
        // Ownership check — throws 404 if not accessible
        ItemList source = getListById(id);

        // Viewer guard
        if (!securityUtils.isAdmin()) {
            WorkspaceMember member = workspaceAccessUtils.requireMembership(source.getWorkspace().getId());
            if (member.getRole() == WorkspaceRole.VIEWER) {
                throw new WorkspaceAccessDeniedException("Viewers cannot duplicate lists");
            }
        }

        UUID userId = requireCurrentUserId();

        // Pessimistic locks to serialize concurrent creation (same pattern as createList)
        User user = userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        workspaceRepository.findByIdWithLock(source.getWorkspace().getId())
                .orElseThrow(() -> new WorkspaceNotFoundException(source.getWorkspace().getId()));

        // Free-tier limit check
        if (premiumEnabled && user.getRole() == Role.USER) {
            long count = itemListRepository.countByUserId(userId);
            if (count >= 5) {
                throw new ListLimitExceededException(
                        "Free plan limited to 5 lists. Upgrade to Premium for unlimited lists.");
            }
        }

        // Clone the list
        ItemList copy = new ItemList();
        // Clamp to 100 chars to match the API-layer name validation constraint
        String rawName = source.getName() + " (Copie)";
        copy.setName(rawName.length() > 100 ? rawName.substring(0, 100) : rawName);
        copy.setDescription(source.getDescription());
        copy.setCategory(source.getCategory());
        copy.setCustomFieldDefinitions(source.getCustomFieldDefinitions());
        // Deliberately attribute the copy to the calling user (not the source list's owner)
        // so the free-tier limit check above, which counts by userId, remains consistent.
        copy.setUser(user);
        copy.setWorkspace(source.getWorkspace());
        ItemList savedCopy = itemListRepository.save(copy);

        // Clone items (skip images — R2 keys are scoped to original item UUIDs)
        List<Item> sourceItems = itemRepository.findAllByItemListIdOrderByPositionAsc(source.getId());
        List<Item> copiedItems = sourceItems.stream().map(src -> {
            Item item = new Item();
            item.setName(src.getName());
            item.setStatus(src.getStatus());
            item.setStock(src.getStock());
            item.setBarcode(src.getBarcode());
            // Defensive copy — prevents shared-reference aliasing between source and clone
            item.setCustomFieldValues(src.getCustomFieldValues() != null ? new HashMap<>(src.getCustomFieldValues()) : null);
            item.setPosition(src.getPosition());
            item.setItemList(savedCopy);
            return item;
        }).toList();
        itemRepository.saveAll(copiedItems);

        return savedCopy;
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
