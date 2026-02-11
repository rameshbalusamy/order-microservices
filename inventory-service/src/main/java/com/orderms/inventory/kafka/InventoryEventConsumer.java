package com.orderms.inventory.kafka;

import com.orderms.inventory.service.InventoryService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Observed
public class InventoryEventConsumer {
    
    private final InventoryService inventoryService;
    
    @KafkaListener(
        topics = "payment-completed", 
        groupId = "inventory-service-group",
        containerFactory = "paymentCompletedKafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for order: {}", event.getOrderId());
        inventoryService.reserveInventory(event);
    }
}
