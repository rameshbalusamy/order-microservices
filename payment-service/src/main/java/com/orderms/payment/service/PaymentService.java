package com.orderms.payment.service;
import com.orderms.payment.kafka.*;
import com.orderms.payment.model.Payment;
import com.orderms.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Random;
import java.util.UUID;

@Service @RequiredArgsConstructor @Slf4j
public class PaymentService {
    private final PaymentRepository repo;
    private final KafkaTemplate<String, Object> kafka;
    private final Random random = new Random();
    
    // Configurable failure rate (default 10%)
    @Value("${payment.failure.rate:10}")
    private int failureRate;
    
    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment for order: {} (Failure rate: {}%)", event.getOrderId(), failureRate);
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = Payment.builder()
            .paymentId(paymentId).orderId(event.getOrderId())
            .customerId(event.getCustomerId()).amount(event.getTotalAmount())
            .status(Payment.PaymentStatus.PENDING).build();
        repo.save(payment);
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        // Configurable success rate: random.nextInt(100) < (100 - failureRate)
        boolean success = random.nextInt(100) >= failureRate;
        
        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
            repo.save(payment);
            kafka.send("payment-completed", event.getOrderId(), PaymentCompletedEvent.builder()
                .orderId(event.getOrderId()).paymentId(paymentId)
                .transactionId(payment.getTransactionId()).amount(event.getTotalAmount())
                .items(event.getItems())  // Pass items to inventory service
                .build());
            log.info("Payment completed: {}", event.getOrderId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            repo.save(payment);
            kafka.send("payment-failed", event.getOrderId(), PaymentFailedEvent.builder()
                .orderId(event.getOrderId()).reason("Payment gateway declined (simulated failure)").build());
            log.error("Payment failed: {}", event.getOrderId());
        }
    }
    
    @Transactional
    public void refundPayment(RefundPaymentCommand cmd) {
        log.info("Refunding payment: {}", cmd.getPaymentId());
        Payment payment = repo.findByPaymentId(cmd.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found: " + cmd.getPaymentId()));
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        repo.save(payment);
        kafka.send("payment-refunded", cmd.getOrderId(), PaymentRefundedEvent.builder()
            .orderId(cmd.getOrderId()).paymentId(cmd.getPaymentId()).build());
        log.info("Payment refunded: {}", cmd.getOrderId());
    }
}
