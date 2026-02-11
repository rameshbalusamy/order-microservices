package com.orderms.order.controller;

import com.orderms.order.model.Order;
import com.orderms.order.model.OrderItem;
import com.orderms.order.service.OrderService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Observed
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("REST: Creating order for customer: {}", request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        log.info("REST: Getting order: {}", orderId);
        OrderResponse response = orderService.getOrder(orderId);
        return ResponseEntity.ok(response);
    }
    
    // DTO Classes
    public static class CreateOrderRequest {
        private String customerId;
        private String customerEmail;
        private Double totalAmount;
        private List<OrderItemDto> items;
        
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public List<OrderItemDto> getItems() { return items; }
        public void setItems(List<OrderItemDto> items) { this.items = items; }
    }
    
    public static class OrderItemDto {
        private String productId;
        private String productName;
        private Integer quantity;
        private Double price;
        
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
    
    public static class OrderResponse {
        private String orderId;
        private String customerId;
        private Double totalAmount;
        private String status;
        private List<OrderItemDto> items;
        private String createdAt;
        private String updatedAt;
        
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public Double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<OrderItemDto> getItems() { return items; }
        public void setItems(List<OrderItemDto> items) { this.items = items; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}
