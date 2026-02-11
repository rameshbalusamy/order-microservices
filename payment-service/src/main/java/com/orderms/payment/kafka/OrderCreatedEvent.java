package com.orderms.payment.kafka;
import lombok.*;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderCreatedEvent {
    private String orderId, customerId, customerEmail;
    private Double totalAmount;
    private List<OrderItemDto> items;
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderItemDto { private String productId, productName; private Integer quantity; private Double price; }
}
