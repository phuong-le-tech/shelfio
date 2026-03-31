package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.BulkDeleteRequest;
import com.inventory.dto.request.ItemRequest;
import com.inventory.dto.request.ItemSearchCriteria;
import com.inventory.dto.request.ReorderItemsRequest;
import com.inventory.dto.response.DashboardStats;
import com.inventory.dto.response.ImageAnalysisResult;
import com.inventory.dto.response.ItemResponse;
import com.inventory.dto.response.PageResponse;
import com.inventory.exception.ItemNotFoundException;
import com.inventory.model.Item;
import com.inventory.service.IImageAnalysisService;
import com.inventory.service.IItemListService;
import com.inventory.service.IItemService;
import com.inventory.service.ImageStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.inventory.exception.RateLimitExceededException;
import com.inventory.security.ApiRateLimiter;
import com.inventory.security.CustomUserDetails;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

import com.inventory.dto.CustomFieldDefinition;
import com.inventory.exception.FileValidationException;
import com.inventory.exception.ServiceUnavailableException;
import com.inventory.util.ImageContentValidator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/items")
@Validated
public class ItemController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "name", "status", "stock", "position");
    private static final int IMAGE_CACHE_MAX_AGE_SECONDS = 840; // 14 minutes (under 15-min presigned URL expiry)
    private static final long MAX_ANALYSIS_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final IItemService itemService;
    private final IItemListService itemListService;
    private final IImageAnalysisService imageAnalysisService;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final ApiRateLimiter uploadRateLimiter;
    private final ImageStorageService imageStorageService;

    public ItemController(IItemService itemService, IItemListService itemListService,
                          IImageAnalysisService imageAnalysisService,
                          ObjectMapper objectMapper, Validator validator,
                          @Qualifier("uploadRateLimiter") ApiRateLimiter uploadRateLimiter,
                          ImageStorageService imageStorageService) {
        this.itemService = itemService;
        this.itemListService = itemListService;
        this.imageAnalysisService = imageAnalysisService;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.uploadRateLimiter = uploadRateLimiter;
        this.imageStorageService = imageStorageService;
    }

    @GetMapping
    public PageResponse<ItemResponse> getAllItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @ModelAttribute @NonNull ItemSearchCriteria criteria) {

        size = Math.min(Math.max(size, 1), 100);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Item> items = itemService.getAllItems(pageable, criteria);

        Page<ItemResponse> responsePage = items.map(item -> ItemResponse.fromEntity(item, imageStorageService));
        return PageResponse.from(responsePage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable @NonNull UUID id) {
        return itemService.getItemById(id)
                .map(item -> ResponseEntity.ok(ItemResponse.fromEntity(item, imageStorageService)))
                .orElseThrow(() -> new ItemNotFoundException(id));
    }

    @GetMapping("/barcode/{code:.+}")
    public ResponseEntity<ItemResponse> getItemByBarcode(
            @PathVariable @NonNull @Size(max = 255, message = "Barcode must not exceed 255 characters") String code) {
        return itemService.getItemByBarcode(code)
                .map(item -> ResponseEntity.ok(ItemResponse.fromEntity(item, imageStorageService)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<?> getItemImage(@PathVariable @NonNull UUID id) {
        Item item = itemService.getItemById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
        if (item.getImageKey() != null) {
            String presignedUrl = imageStorageService.getPresignedUrl(item.getImageKey());
            if (presignedUrl != null) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", presignedUrl)
                        .header("Cache-Control", "private, max-age=" + IMAGE_CACHE_MAX_AGE_SECONDS)
                        .build();
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemResponse> createItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("data") @NonNull String data,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        if (!uploadRateLimiter.tryAcquire("upload:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many upload requests. Please try again later.");
        }
        ItemRequest request;
        try {
            request = objectMapper.readValue(data, ItemRequest.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid request data format: " + e.getMessage(), e);
        }
        Objects.requireNonNull(request, "Item request data must not be null");
        Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Item item = itemService.createItem(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ItemResponse.fromEntity(item, imageStorageService));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse updateItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @NonNull UUID id,
            @RequestParam("data") @NonNull String data,
            @RequestParam(value = "image", required = false) MultipartFile image) throws IOException {
        if (!uploadRateLimiter.tryAcquire("upload:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many upload requests. Please try again later.");
        }
        ItemRequest request;
        try {
            request = objectMapper.readValue(data, ItemRequest.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid request data format: " + e.getMessage(), e);
        }
        Objects.requireNonNull(request, "Item request data must not be null");
        Set<ConstraintViolation<ItemRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        Item item = itemService.updateItem(id, request, image);
        return ItemResponse.fromEntity(item, imageStorageService);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable @NonNull UUID id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<Void> bulkDeleteItems(@RequestBody @jakarta.validation.Valid BulkDeleteRequest request) {
        itemService.deleteItems(Objects.requireNonNull(request.getIds()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderItems(@RequestBody @jakarta.validation.Valid ReorderItemsRequest request) {
        itemService.reorderItems(Objects.requireNonNull(request));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public DashboardStats getDashboardStats() {
        return itemService.getDashboardStats();
    }

    @GetMapping("/analyze-image/status")
    public ResponseEntity<Map<String, Boolean>> getAnalysisStatus() {
        return ResponseEntity.ok(Map.of("available", imageAnalysisService.isAvailable()));
    }

    @PostMapping(value = "/analyze-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageAnalysisResult> analyzeImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "listId", required = false) UUID listId) throws IOException {
        if (!imageAnalysisService.isAvailable()) {
            throw new ServiceUnavailableException("AI analysis not available");
        }
        if (image.isEmpty()) {
            throw new FileValidationException("Image file is required");
        }
        if (image.getSize() > MAX_ANALYSIS_FILE_SIZE) {
            throw new FileValidationException("Image exceeds 10MB limit");
        }
        if (!uploadRateLimiter.tryAcquire("upload:user:" + userDetails.getId()).allowed()) {
            throw new RateLimitExceededException("Too many analysis requests. Please try again later.");
        }
        List<CustomFieldDefinition> fieldDefinitions = Collections.emptyList();
        if (listId != null) {
            // getListById checks workspace membership — prevents IDOR
            List<CustomFieldDefinition> listFields = itemListService.getListById(listId).getCustomFieldDefinitions();
            if (listFields != null) {
                fieldDefinitions = listFields;
            }
        }

        byte[] imageBytes = image.getBytes();
        if (!ImageContentValidator.isValidImage(imageBytes)) {
            throw new FileValidationException("Invalid image format. Allowed: JPEG, PNG, GIF, WebP");
        }
        String analysisId = imageAnalysisService.analyzeImage(imageBytes, fieldDefinitions, userDetails.getId());
        return ResponseEntity.accepted().body(ImageAnalysisResult.pending(analysisId));
    }

    @GetMapping("/analyze-image/{analysisId}")
    public ResponseEntity<ImageAnalysisResult> getAnalysisResult(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @NonNull UUID analysisId) {
        return imageAnalysisService.getResult(analysisId.toString(), userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
