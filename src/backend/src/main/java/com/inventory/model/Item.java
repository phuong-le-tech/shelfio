package com.inventory.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.inventory.enums.ItemStatus;

@Data
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_list_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ItemList itemList;

    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.TO_PREPARE;

    @Column(nullable = false)
    private Integer stock = 0;

    @Type(JsonType.class)
    @Column(name = "custom_field_values", columnDefinition = "json")
    private Map<String, Object> customFieldValues;

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
