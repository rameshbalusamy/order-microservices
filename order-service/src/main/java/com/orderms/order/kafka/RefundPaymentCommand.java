package com.orderms.order.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPaymentCommand {
    private String orderId;
    private String paymentId;
    private String reason;
}
