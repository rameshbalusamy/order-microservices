package com.orderms.payment.kafka;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundPaymentCommand {
    private String orderId, paymentId, reason;
}
