package com.orderms.order.service;

import com.orderms.order.controller.OrderController.CreateOrderRequest;
import com.orderms.order.controller.OrderController.OrderResponse;
import com.orderms.order.controller.OrderController.OrderItemDto;
import com.orderms.order.kafka.*;
import com.orderms.order.model.Order;
import com.orderms.order.model.OrderItem;
import com.orderms.order.repository.OrderRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    // Store SSE emitters for real-time updates
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String REFUND_PAYMENT_TOPIC = "refund-payment";
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        // Generate unique order ID
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Create order entity
        Order order = Order.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .totalAmount(request.getTotalAmount())
                .status(Order.OrderStatus.PENDING)
                .build();
        
        // Add items to order
        request.getItems().forEach(itemDto -> {
            OrderItem item = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .productName(itemDto.getProductName())
                    .quantity(itemDto.getQuantity())
                    .price(itemDto.getPrice())
                    .build();
            order.addItem(item);
        });
        
        // Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", orderId);
        
        // Update status to PAYMENT_PROCESSING
        updateOrderStatus(savedOrder, Order.OrderStatus.PAYMENT_PROCESSING);
        
        // Publish order created event to Kafka (triggers payment)
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .totalAmount(request.getTotalAmount())
                .items(request.getItems().stream()
                        .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        
        kafkaTemplate.send(ORDER_CREATED_TOPIC, orderId, event);
        log.info("Published OrderCreatedEvent for order: {}", orderId);
        
        return mapToResponse(savedOrder);
    }
    
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Payment completed for order: {}", event.getOrderId());
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
        
        order.setPaymentId(event.getPaymentId());
        updateOrderStatus(order, Order.OrderStatus.PAYMENT_COMPLETED);
        
        // Move to inventory reservation phase
        updateOrderStatus(order, Order.OrderStatus.INVENTORY_RESERVING);
        
        sendStatusUpdate(event.getOrderId(), "Payment completed successfully");
    }
    
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("Payment failed for order: {}. Reason: {}", event.getOrderId(), event.getReason());
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
        
        updateOrderStatus(order, Order.OrderStatus.PAYMENT_FAILED);
        updateOrderStatus(order, Order.OrderStatus.CANCELLED);
        
        sendStatusUpdate(event.getOrderId(), "Payment failed: " + event.getReason());
    }
    
    @Transactional
    public void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Inventory reserved for order: {}", event.getOrderId());
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
        
        order.setReservationId(event.getReservationId());
        updateOrderStatus(order, Order.OrderStatus.INVENTORY_RESERVED);
        
        // Move to notification phase
        updateOrderStatus(order, Order.OrderStatus.NOTIFYING);
        
        sendStatusUpdate(event.getOrderId(), "Inventory reserved successfully");
    }
    
    @Transactional
    public void handleInventoryFailed(InventoryFailedEvent event) {
        log.error("Inventory reservation failed for order: {}. Reason: {}", 
                event.getOrderId(), event.getReason());
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
        
        updateOrderStatus(order, Order.OrderStatus.INVENTORY_FAILED);
        
        // SAGA Compensation: Refund payment
        if (order.getPaymentId() != null) {
            log.info("Initiating payment refund for order: {}", event.getOrderId());
            
            RefundPaymentCommand refundCommand = RefundPaymentCommand.builder()
                    .orderId(event.getOrderId())
                    .paymentId(order.getPaymentId())
                    .reason("Inventory reservation failed")
                    .build();
            
            kafkaTemplate.send(REFUND_PAYMENT_TOPIC, event.getOrderId(), refundCommand);
            log.info("Refund command sent for order: {}", event.getOrderId());
        }
        
        updateOrderStatus(order, Order.OrderStatus.CANCELLED);
        
        sendStatusUpdate(event.getOrderId(), 
                "Inventory reservation failed. Payment refunded: " + event.getReason());
    }
    
    @Transactional
    public void handleNotificationSent(NotificationSentEvent event) {
        log.info("Notification sent for order: {}", event.getOrderId());
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));
        
        updateOrderStatus(order, Order.OrderStatus.COMPLETED);
        
        sendStatusUpdate(event.getOrderId(), "Order completed successfully!");
    }
    
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        return mapToResponse(order);
    }
    
    public SseEmitter streamOrderStatus(String orderId) {
        log.info("Creating SSE stream for order: {}", orderId);
        
        // Verify order exists
        orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(orderId, emitter);
        
        emitter.onCompletion(() -> {
            log.info("SSE completed for order: {}", orderId);
            emitters.remove(orderId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE timeout for order: {}", orderId);
            emitters.remove(orderId);
        });
        
        emitter.onError((e) -> {
            log.error("SSE error for order: {}", orderId, e);
            emitters.remove(orderId);
        });
        
        // Send initial status
        try {
            Order order = orderRepository.findByOrderId(orderId).get();
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data("Connected. Current status: " + order.getStatus()));
        } catch (IOException e) {
            log.error("Error sending initial SSE event", e);
        }
        
        return emitter;
    }
    
    private void updateOrderStatus(Order order, Order.OrderStatus newStatus) {
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Order {} status updated to: {}", order.getOrderId(), newStatus);
    }
    
    private void sendStatusUpdate(String orderId, String message) {
        SseEmitter emitter = emitters.get(orderId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(message));
            } catch (IOException e) {
                log.error("Error sending SSE update for order: {}", orderId, e);
                emitters.remove(orderId);
            }
        }
    }
    
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setCustomerId(order.getCustomerId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus().name());
        response.setCreatedAt(order.getCreatedAt().toString());
        response.setUpdatedAt(order.getUpdatedAt().toString());
        
        response.setItems(order.getItems().stream()
                .map(item -> {
                    OrderItemDto itemDto = new OrderItemDto();
                    itemDto.setProductId(item.getProductId());
                    itemDto.setProductName(item.getProductName());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setPrice(item.getPrice());
                    return itemDto;
                })
                .collect(Collectors.toList()));
        
        return response;
    }
}
