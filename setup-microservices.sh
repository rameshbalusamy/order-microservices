#!/bin/bash

# This script generates all the remaining microservice code
# Payment Service, Inventory Service, and Notification Service

set -e

BASE_DIR="/home/claude/order-microservices-saga"

echo "========================================="
echo "Generating Payment Service..."
echo "========================================="

# Payment Service - Application YAML
cat > "$BASE_DIR/payment-service/src/main/resources/application.yml" << 'EOF'
spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:postgresql://localhost:5433/paymentdb
    username: paymentuser
    password: paymentpass
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: payment-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

server:
  port: 8081

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: jdbc:postgresql://postgres-payment:5432/paymentdb
  kafka:
    bootstrap-servers: kafka:9092
management:
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans
EOF

# Payment Model
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/model/Payment.java" << 'EOF'
package com.orderms.payment.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String paymentId;
    
    @Column(nullable = false)
    private String orderId;
    
    @Column(nullable = false)
    private String customerId;
    
    @Column(nullable = false)
    private Double amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
EOF

# Payment Repository
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/repository/PaymentRepository.java" << 'EOF'
package com.orderms.payment.repository;

import com.orderms.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByOrderId(String orderId);
}
EOF

# Payment Events
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/OrderCreatedEvent.java" << 'EOF'
package com.orderms.payment.kafka;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent {
    private String orderId;
    private String customerId;
    private String customerEmail;
    private Double totalAmount;
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
EOF

cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/PaymentCompletedEvent.java" << 'EOF'
package com.orderms.payment.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {
    private String orderId;
    private String paymentId;
    private String transactionId;
    private Double amount;
}
EOF

cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/PaymentFailedEvent.java" << 'EOF'
package com.orderms.payment.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentFailedEvent {
    private String orderId;
    private String reason;
}
EOF

cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/RefundPaymentCommand.java" << 'EOF'
package com.orderms.payment.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPaymentCommand {
    private String orderId;
    private String paymentId;
    private String reason;
}
EOF

cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/PaymentRefundedEvent.java" << 'EOF'
package com.orderms.payment.kafka;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundedEvent {
    private String orderId;
    private String paymentId;
}
EOF

# Payment Service
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/service/PaymentService.java" << 'EOF'
package com.orderms.payment.service;

import com.orderms.payment.kafka.*;
import com.orderms.payment.model.Payment;
import com.orderms.payment.repository.PaymentRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Observed
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    
    @Transactional
    public void processPayment(OrderCreatedEvent event) {
        log.info("Processing payment for order: {}", event.getOrderId());
        
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(event.getTotalAmount())
                .status(Payment.PaymentStatus.PENDING)
                .build();
        
        paymentRepository.save(payment);
        
        // Simulate payment processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 90% success rate
        boolean success = random.nextInt(10) < 9;
        
        if (success) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8));
            paymentRepository.save(payment);
            
            PaymentCompletedEvent completedEvent = PaymentCompletedEvent.builder()
                    .orderId(event.getOrderId())
                    .paymentId(paymentId)
                    .transactionId(payment.getTransactionId())
                    .amount(event.getTotalAmount())
                    .build();
            
            kafkaTemplate.send("payment-completed", event.getOrderId(), completedEvent);
            log.info("Payment completed for order: {}", event.getOrderId());
        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            PaymentFailedEvent failedEvent = PaymentFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .reason("Payment gateway declined")
                    .build();
            
            kafkaTemplate.send("payment-failed", event.getOrderId(), failedEvent);
            log.error("Payment failed for order: {}", event.getOrderId());
        }
    }
    
    @Transactional
    public void refundPayment(RefundPaymentCommand command) {
        log.info("Refunding payment: {} for order: {}", command.getPaymentId(), command.getOrderId());
        
        Payment payment = paymentRepository.findByPaymentId(command.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + command.getPaymentId()));
        
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        
        PaymentRefundedEvent refundedEvent = PaymentRefundedEvent.builder()
                .orderId(command.getOrderId())
                .paymentId(command.getPaymentId())
                .build();
        
        kafkaTemplate.send("payment-refunded", command.getOrderId(), refundedEvent);
        log.info("Payment refunded successfully for order: {}", command.getOrderId());
    }
}
EOF

# Payment Consumer
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/kafka/PaymentEventConsumer.java" << 'EOF'
package com.orderms.payment.kafka;

import com.orderms.payment.service.PaymentService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Observed
public class PaymentEventConsumer {
    
    private final PaymentService paymentService;
    
    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());
        paymentService.processPayment(event);
    }
    
    @KafkaListener(topics = "refund-payment", groupId = "payment-service-group")
    public void handleRefundPayment(RefundPaymentCommand command) {
        log.info("Received RefundPaymentCommand for order: {}", command.getOrderId());
        paymentService.refundPayment(command);
    }
}
EOF

# Payment Application
cat > "$BASE_DIR/payment-service/src/main/java/com/orderms/payment/PaymentServiceApplication.java" << 'EOF'
package com.orderms.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
EOF

# Payment Dockerfile
cat > "$BASE_DIR/payment-service/Dockerfile" << 'EOF'
FROM maven:3.9.5-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
EOF

echo "Payment Service generated successfully!"

echo "========================================="
echo "Generating Inventory Service..."
echo "========================================="

# Create Inventory Service directories
mkdir -p "$BASE_DIR/inventory-service/src/main/java/com/orderms/inventory/{model,repository,service,kafka}"
mkdir -p "$BASE_DIR/inventory-service/src/main/resources"

# Inventory POM
cat > "$BASE_DIR/inventory-service/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    <groupId>com.orderms</groupId>
    <artifactId>inventory-service</artifactId>
    <version>1.0.0</version>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
EOF

# Inventory Application YAML
cat > "$BASE_DIR/inventory-service/src/main/resources/application.yml" << 'EOF'
spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:postgresql://localhost:5434/inventorydb
    username: inventoryuser
    password: inventorypass
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  kafka:
    bootstrap-servers: localhost:29092
    consumer:
      group-id: inventory-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

server:
  port: 8082

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

---
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: jdbc:postgresql://postgres-inventory:5432/inventorydb
  kafka:
    bootstrap-servers: kafka:9092
management:
  zipkin:
    tracing:
      endpoint: http://jaeger:9411/api/v2/spans
EOF

# Continue script in next message due to length...
chmod +x "$BASE_DIR/setup-microservices.sh"

echo "Script created successfully!"
EOF
