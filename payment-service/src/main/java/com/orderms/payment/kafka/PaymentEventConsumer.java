package com.orderms.payment.kafka;
import com.orderms.payment.service.PaymentService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j @Observed
public class PaymentEventConsumer {
    private final PaymentService service;
    
    @KafkaListener(
        topics = "order-created", 
        groupId = "payment-service-group",
        containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent: {}", event.getOrderId());
        service.processPayment(event);
    }
    
    @KafkaListener(topics = "refund-payment", groupId = "payment-service-group")
    public void handleRefund(RefundPaymentCommand cmd) {
        log.info("Received RefundPaymentCommand: {}", cmd.getOrderId());
        service.refundPayment(cmd);
    }
}
