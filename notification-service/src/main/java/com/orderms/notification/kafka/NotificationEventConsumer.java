package com.orderms.notification.kafka;

import com.orderms.notification.service.NotificationService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Observed
public class NotificationEventConsumer {
    
    private final NotificationService notificationService;
    
    @KafkaListener(
        topics = "inventory-reserved", 
        groupId = "notification-service-group",
        containerFactory = "inventoryReservedKafkaListenerContainerFactory"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for order: {}", event.getOrderId());
        notificationService.sendOrderConfirmation(event);
    }
}
