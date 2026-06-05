package com.kpiso.api.modules.shoppinglist;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_items", indexes = {
        @Index(name = "idx_house_id", columnList = "house_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_house_status", columnList = "house_id,status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "house_id", nullable = false)
    private UUID houseId;

    @Column(name = "added_by_id", nullable = false)
    private UUID addedById;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "estimated_price", nullable = false)
    private Double estimatedPrice;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    private ShoppingItemStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "checkout_id")
    private UUID checkoutId;

    @Column(name = "quantity")
    private Integer quantity;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "shopping_item_assigned_users", joinColumns = @JoinColumn(name = "shopping_item_id"))
    @Column(name = "user_id")
    private List<UUID> assignedUsers;

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
