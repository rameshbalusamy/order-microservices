package com.orderms.inventory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private String productName;
    
    @Column(nullable = false)
    private Integer availableQuantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity;
    
    private LocalDateTime createdAt;
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
    
    public boolean hasAvailableStock(int quantity) {
        return availableQuantity >= quantity;
    }
    
    public void reserveStock(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new RuntimeException("Insufficient stock for product: " + productId);
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }
    
    public void releaseStock(int quantity) {
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }
}
