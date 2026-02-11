package com.orderms.payment.kafka;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentFailedEvent {
    private String orderId, reason;
}
