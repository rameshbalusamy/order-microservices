package com.orderms.payment.repository;
import com.orderms.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByOrderId(String orderId);
}
