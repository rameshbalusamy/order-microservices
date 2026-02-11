package com.orderms.order.kafka;

import com.orderms.order.service.OrderService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Observed
public class OrderEventConsumer {
    
    private final OrderService orderService;
    
    @KafkaListener(
        topics = "payment-completed", 
        groupId = "order-service-group",
        containerFactory = "paymentCompletedKafkaListenerContainerFactory"
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Received PaymentCompletedEvent for order: {}", event.getOrderId());
        orderService.handlePaymentCompleted(event);
    }
    
    @KafkaListener(
        topics = "payment-failed", 
        groupId = "order-service-group",
        containerFactory = "paymentFailedKafkaListenerContainerFactory"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent for order: {}", event.getOrderId());
        orderService.handlePaymentFailed(event);
    }
    
    @KafkaListener(
        topics = "inventory-reserved", 
        groupId = "order-service-group",
        containerFactory = "inventoryReservedKafkaListenerContainerFactory"
    )
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReservedEvent for order: {}", event.getOrderId());
        orderService.handleInventoryReserved(event);
    }
    
    @KafkaListener(
        topics = "inventory-failed", 
        groupId = "order-service-group",
        containerFactory = "inventoryFailedKafkaListenerContainerFactory"
    )
    public void handleInventoryFailed(InventoryFailedEvent event) {
        log.info("Received InventoryFailedEvent for order: {}", event.getOrderId());
        orderService.handleInventoryFailed(event);
    }
    
    @KafkaListener(
        topics = "notification-sent", 
        groupId = "order-service-group",
        containerFactory = "notificationSentKafkaListenerContainerFactory"
    )
    public void handleNotificationSent(NotificationSentEvent event) {
        log.info("Received NotificationSentEvent for order: {}", event.getOrderId());
        orderService.handleNotificationSent(event);
    }
}
