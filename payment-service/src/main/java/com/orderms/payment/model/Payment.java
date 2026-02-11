package com.orderms.payment.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true) private String paymentId;
    private String orderId, customerId, transactionId;
    private Double amount;
    @Enumerated(EnumType.STRING) private PaymentStatus status;
    private LocalDateTime createdAt, updatedAt;
    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
    public enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }
}
