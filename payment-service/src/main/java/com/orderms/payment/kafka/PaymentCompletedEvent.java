package com.orderms.payment.kafka;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentCompletedEvent {
    private String orderId, paymentId, transactionId;
    private Double amount;
    private List<OrderCreatedEvent.OrderItemDto> items;
}
