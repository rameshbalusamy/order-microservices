package com.orderms.notification.service;

import com.orderms.notification.kafka.InventoryReservedEvent;
import com.orderms.notification.kafka.NotificationSentEvent;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class NotificationService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void sendOrderConfirmation(InventoryReservedEvent event) {
        log.info("Sending order confirmation notification for order: {}", event.getOrderId());
        
        try {
            // Simulate email sending
            Thread.sleep(100);
            
            // In a real implementation, this would:
            // 1. Fetch customer email from order service or database
            // 2. Compose email with order details
            // 3. Send via email service (SendGrid, AWS SES, etc.)
            // 4. Log the notification
            
            log.info("📧 EMAIL SENT: Order {} confirmed and ready for processing", event.getOrderId());
            log.info("   Subject: Your Order Confirmation - {}", event.getOrderId());
            log.info("   Body: Thank you for your order! Your items have been reserved.");
            
            // Publish notification sent event
            String notificationId = "NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            NotificationSentEvent sentEvent = NotificationSentEvent.builder()
                    .orderId(event.getOrderId())
                    .notificationId(notificationId)
                    .build();
            
            kafkaTemplate.send("notification-sent", event.getOrderId(), sentEvent);
            log.info("Notification sent successfully for order: {}", event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to send notification for order: {}", event.getOrderId(), e);
            // In production, you might want to:
            // - Retry sending
            // - Save to dead letter queue
            // - Alert monitoring system
        }
    }
    
    public void sendOrderCancellation(String orderId, String reason) {
        log.info("Sending order cancellation notification for order: {}", orderId);
        
        log.info("📧 EMAIL SENT: Order {} has been cancelled", orderId);
        log.info("   Subject: Order Cancellation - {}", orderId);
        log.info("   Body: Your order has been cancelled. Reason: {}", reason);
        log.info("   Any charges have been refunded to your account.");
    }
}
