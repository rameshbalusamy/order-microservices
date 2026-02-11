package com.orderms.inventory.service;

import com.orderms.inventory.kafka.*;
import com.orderms.inventory.model.InventoryItem;
import com.orderms.inventory.model.InventoryReservation;
import com.orderms.inventory.repository.InventoryItemRepository;
import com.orderms.inventory.repository.InventoryReservationRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class InventoryService {
    
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    
    // Configurable failure rate (default 5%)
    @Value("${inventory.failure.rate:5}")
    private int failureRate;
    
    @PostConstruct
    public void initializeInventory() {
        // Initialize some sample inventory if database is empty
        if (inventoryItemRepository.count() == 0) {
            log.info("Initializing sample inventory...");
            
            inventoryItemRepository.save(InventoryItem.builder()
                    .productId("PROD-001")
                    .productName("Laptop")
                    .availableQuantity(100)
                    .reservedQuantity(0)
                    .build());
            
            inventoryItemRepository.save(InventoryItem.builder()
                    .productId("PROD-LAPTOP-001")
                    .productName("Dell XPS 15 Laptop")
                    .availableQuantity(50)
                    .reservedQuantity(0)
                    .build());
            
            inventoryItemRepository.save(InventoryItem.builder()
                    .productId("PROD-MONITOR-001")
                    .productName("4K Monitor 27inch")
                    .availableQuantity(75)
                    .reservedQuantity(0)
                    .build());
            
            inventoryItemRepository.save(InventoryItem.builder()
                    .productId("PROD-KEYBOARD-001")
                    .productName("Mechanical Keyboard")
                    .availableQuantity(200)
                    .reservedQuantity(0)
                    .build());
            
            log.info("Sample inventory initialized successfully");
        }
    }
    
    @Transactional
    public void reserveInventory(PaymentCompletedEvent event) {
        log.info("Reserving inventory for order: {} (Failure rate: {}%)", event.getOrderId(), failureRate);
        
        String reservationId = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        try {
            // First: Configurable failure simulation for testing
            if (random.nextInt(100) < failureRate) {
                log.warn("Simulating inventory failure for order: {} (triggered by {}% failure rate)", 
                    event.getOrderId(), failureRate);
                throw new RuntimeException("Simulated inventory failure for testing SAGA compensation");
            }
            
            // Second: REAL inventory validation
            if (event.getItems() == null || event.getItems().isEmpty()) {
                throw new RuntimeException("No items to reserve");
            }
            
            // Check and reserve each item
            for (PaymentCompletedEvent.OrderItemDto itemDto : event.getItems()) {
                log.info("Checking inventory for product: {} (quantity: {})", itemDto.getProductId(), itemDto.getQuantity());
                
                InventoryItem item = inventoryItemRepository.findByProductId(itemDto.getProductId())
                        .orElseThrow(() -> new RuntimeException(
                            "Product not found in inventory: " + itemDto.getProductId() + 
                            " (Product Name: " + itemDto.getProductName() + ")"));
                
                if (!item.hasAvailableStock(itemDto.getQuantity())) {
                    throw new RuntimeException(
                        String.format("Insufficient stock for product: %s. Available: %d, Requested: %d",
                            itemDto.getProductId(), item.getAvailableQuantity(), itemDto.getQuantity()));
                }
                
                // Reserve the stock
                log.info("Reserving {} units of product: {} (current available: {})", 
                    itemDto.getQuantity(), itemDto.getProductId(), item.getAvailableQuantity());
                item.reserveStock(itemDto.getQuantity());
                inventoryItemRepository.save(item);
                
                // Create reservation record
                InventoryReservation reservation = InventoryReservation.builder()
                        .reservationId(reservationId + "-" + itemDto.getProductId())
                        .orderId(event.getOrderId())
                        .productId(itemDto.getProductId())
                        .quantity(itemDto.getQuantity())
                        .status(InventoryReservation.ReservationStatus.RESERVED)
                        .build();
                
                reservationRepository.save(reservation);
                log.info("Reserved {} units of product: {}", itemDto.getQuantity(), itemDto.getProductId());
            }
            
            // Simulate processing time
            Thread.sleep(500);
            
            // Publish success event
            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .reservationId(reservationId)
                    .build();
            
            kafkaTemplate.send("inventory-reserved", event.getOrderId(), reservedEvent);
            log.info("All inventory reserved successfully for order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Inventory reservation failed for order: {} - Reason: {}", event.getOrderId(), e.getMessage());
            
            // Save failed reservation
            InventoryReservation failedReservation = InventoryReservation.builder()
                    .reservationId(reservationId)
                    .orderId(event.getOrderId())
                    .productId("FAILED")
                    .quantity(0)
                    .status(InventoryReservation.ReservationStatus.FAILED)
                    .build();
            
            reservationRepository.save(failedReservation);
            
            // Publish failure event
            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason(e.getMessage())
                    .build();
            
            kafkaTemplate.send("inventory-failed", event.getOrderId(), failedEvent);
        }
    }
    
    @Transactional
    public void releaseInventory(String orderId) {
        log.info("Releasing inventory for order: {}", orderId);
        
        // Find all reservations for this order
        var reservations = reservationRepository.findByOrderId(orderId);
        
        for (InventoryReservation reservation : reservations) {
            if (reservation.getStatus() == InventoryReservation.ReservationStatus.RESERVED) {
                // Release the stock
                inventoryItemRepository.findByProductId(reservation.getProductId())
                        .ifPresent(item -> item.releaseStock(reservation.getQuantity()));
                
                // Update reservation status
                reservation.setStatus(InventoryReservation.ReservationStatus.RELEASED);
                reservationRepository.save(reservation);
            }
        }
        
        log.info("Inventory released for order: {}", orderId);
    }
}
