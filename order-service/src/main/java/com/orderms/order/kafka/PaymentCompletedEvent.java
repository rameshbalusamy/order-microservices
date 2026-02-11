package com.orderms.order.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private String orderId;
    private String paymentId;
    private String transactionId;
    private Double amount;
    private List<OrderItemDto> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
    }
}
