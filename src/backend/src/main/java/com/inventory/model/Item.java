package com.inventory.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

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
    private ItemStatus status = ItemStatus.AVAILABLE;

    @Column(nullable = false)
    private Integer stock = 0;

    @Type(JsonType.class)
    @Column(name = "custom_field_values", columnDefinition = "json")
    private Map<String, Object> customFieldValues;

    @Column(length = 255)
    private String barcode;

    @Column(name = "image_key", length = 500)
    private String imageKey;

    @Column(nullable = false)
    private Integer position = 0;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
