package com.orderms.order.controller;

import com.orderms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderStreamController {
    
    private final OrderService orderService;
    
    @GetMapping(value = "/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderStatus(@PathVariable String orderId) {
        log.info("SSE: Streaming status updates for order: {}", orderId);
        return orderService.streamOrderStatus(orderId);
    }
}
