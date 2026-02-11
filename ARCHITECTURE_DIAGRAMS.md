# System Architecture Diagrams

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        A[Postman/Browser]
    end
    
    subgraph "API Gateway Layer"
        B[Order Service :8080<br/>SAGA Orchestrator]
    end
    
    subgraph "Event Bus"
        K[Apache Kafka<br/>Topics: order-created, payment-*, inventory-*, notification-*]
    end
    
    subgraph "Service Layer"
        C[Payment Service :8081]
        D[Inventory Service :8082]
        E[Notification Service :8083]
    end
    
    subgraph "Data Layer"
        DB1[(PostgreSQL<br/>Order DB)]
        DB2[(PostgreSQL<br/>Payment DB)]
        DB3[(PostgreSQL<br/>Inventory DB)]
    end
    
    subgraph "Observability Stack"
        P[Prometheus<br/>Metrics]
        G[Grafana<br/>Dashboards]
        J[Jaeger<br/>Tracing]
    end
    
    A -->|HTTP REST| B
    A -.->|SSE Stream| B
    B <-->|Events| K
    C <-->|Events| K
    D <-->|Events| K
    E <-->|Events| K
    
    B --> DB1
    C --> DB2
    D --> DB3
    
    B -.->|Metrics| P
    C -.->|Metrics| P
    D -.->|Metrics| P
    E -.->|Metrics| P
    
    P --> G
    B -.->|Traces| J
    C -.->|Traces| J
    D -.->|Traces| J
    E -.->|Traces| J
```

## SAGA Flow - Happy Path

```mermaid
sequenceDiagram
    participant C as Client
    participant O as Order Service
    participant K as Kafka
    participant P as Payment Service
    participant I as Inventory Service
    participant N as Notification Service
    
    C->>O: POST /api/orders
    activate O
    O->>O: Create Order (PENDING)
    O->>K: publish order-created
    O-->>C: 201 Created {orderId}
    O->>O: Status: PAYMENT_PROCESSING
    deactivate O
    
    K->>P: order-created event
    activate P
    P->>P: Process Payment (1s)
    P->>P: Save Payment (COMPLETED)
    P->>K: publish payment-completed
    deactivate P
    
    K->>O: payment-completed event
    activate O
    O->>O: Status: PAYMENT_COMPLETED
    O->>O: Status: INVENTORY_RESERVING
    deactivate O
    
    K->>I: payment-completed event
    activate I
    I->>I: Check Stock
    I->>I: Reserve Inventory
    I->>K: publish inventory-reserved
    deactivate I
    
    K->>O: inventory-reserved event
    activate O
    O->>O: Status: INVENTORY_RESERVED
    O->>O: Status: NOTIFYING
    deactivate O
    
    K->>N: inventory-reserved event
    activate N
    N->>N: Send Email
    N->>K: publish notification-sent
    deactivate N
    
    K->>O: notification-sent event
    activate O
    O->>O: Status: COMPLETED ✅
    deactivate O
```

## SAGA Flow - Compensation (Inventory Failure)

```mermaid
sequenceDiagram
    participant C as Client
    participant O as Order Service
    participant K as Kafka
    participant P as Payment Service
    participant I as Inventory Service
    
    C->>O: POST /api/orders
    O->>O: Create Order (PENDING)
    O->>K: publish order-created
    O-->>C: 201 Created
    
    K->>P: order-created
    P->>P: Process Payment ✅
    P->>K: publish payment-completed
    
    K->>O: payment-completed
    O->>O: Status: PAYMENT_COMPLETED
    
    K->>I: payment-completed
    I->>I: Check Stock
    I->>I: ❌ INSUFFICIENT STOCK
    I->>K: publish inventory-failed
    
    K->>O: inventory-failed
    activate O
    O->>O: Status: INVENTORY_FAILED
    rect rgb(255, 200, 200)
        Note over O: SAGA COMPENSATION
        O->>O: Check Payment ID exists
        O->>K: publish refund-payment
    end
    deactivate O
    
    K->>P: refund-payment
    activate P
    rect rgb(255, 200, 200)
        Note over P: Compensating Transaction
        P->>P: Refund Payment
        P->>P: Status: REFUNDED
        P->>K: publish payment-refunded
    end
    deactivate P
    
    K->>O: payment-refunded
    O->>O: Status: CANCELLED ❌
```

## Service Communication Patterns

```mermaid
graph LR
    subgraph "Synchronous"
        A[Client] -->|HTTP/REST| B[Order Service]
        A -.->|SSE| B
    end
    
    subgraph "Asynchronous Event-Driven"
        B -->|Kafka| C[Payment Service]
        C -->|Kafka| B
        B -->|Kafka| D[Inventory Service]
        D -->|Kafka| B
        D -->|Kafka| E[Notification Service]
        E -->|Kafka| B
        B -->|Kafka Compensation| C
    end
    
    style A fill:#e1f5ff
    style B fill:#ffe1e1
    style C fill:#e1ffe1
    style D fill:#e1ffe1
    style E fill:#e1ffe1
```

## Data Flow

```mermaid
flowchart TD
    A[Order Request] --> B{Order Service}
    B --> C[Save to Order DB]
    B --> D[Publish to Kafka]
    D --> E[Payment Service]
    E --> F{Payment Success?}
    F -->|Yes| G[Update Payment DB]
    F -->|No| H[Publish Payment Failed]
    G --> I[Publish Payment Completed]
    I --> J[Inventory Service]
    J --> K{Stock Available?}
    K -->|Yes| L[Update Inventory DB]
    K -->|No| M[Publish Inventory Failed]
    L --> N[Publish Inventory Reserved]
    M --> O[COMPENSATION: Refund Payment]
    O --> P[Cancel Order]
    N --> Q[Notification Service]
    Q --> R[Send Email]
    R --> S[Publish Notification Sent]
    S --> T[Complete Order]
    
    style F fill:#ffe1e1
    style K fill:#ffe1e1
    style M fill:#ff9999
    style O fill:#ff9999
    style P fill:#ff9999
```

## Technology Stack

```mermaid
graph TB
    subgraph "Application Layer"
        A1[Spring Boot 3.2]
        A2[Java 17]
        A3[Maven]
    end
    
    subgraph "API Layer"
        B1[OpenAPI 3.0]
        B2[REST]
        B3[SSE]
    end
    
    subgraph "Messaging"
        C1[Apache Kafka 7.5]
        C2[Zookeeper]
    end
    
    subgraph "Persistence"
        D1[PostgreSQL 15]
        D2[JPA/Hibernate]
    end
    
    subgraph "Observability"
        E1[Jaeger Tracing]
        E2[Prometheus Metrics]
        E3[Grafana Dashboards]
    end
    
    subgraph "Infrastructure"
        F1[Docker]
        F2[Docker Compose]
    end
```

## Deployment Architecture

```mermaid
graph TB
    subgraph "Docker Compose Environment"
        subgraph "Network: microservices-network"
            subgraph "Application Services"
                O[order-service:8080]
                P[payment-service:8081]
                I[inventory-service:8082]
                N[notification-service:8083]
            end
            
            subgraph "Infrastructure Services"
                K[kafka:9092]
                Z[zookeeper:2181]
            end
            
            subgraph "Databases"
                DB1[postgres-order:5432]
                DB2[postgres-payment:5433]
                DB3[postgres-inventory:5434]
            end
            
            subgraph "Monitoring"
                PR[prometheus:9090]
                GR[grafana:3000]
                JA[jaeger:16686]
            end
        end
    end
    
    O --> K
    P --> K
    I --> K
    N --> K
    K --> Z
    
    O --> DB1
    P --> DB2
    I --> DB3
    
    O -.-> PR
    P -.-> PR
    I -.-> PR
    N -.-> PR
    
    PR --> GR
    
    O -.-> JA
    P -.-> JA
    I -.-> JA
    N -.-> JA
```

---

## Notes

- **Solid Lines**: Synchronous/Direct communication
- **Dashed Lines**: Asynchronous/Monitoring
- **Red/Pink Boxes**: Error/Compensation paths
- **Green Boxes**: Success paths
- **Blue Boxes**: Entry points

All diagrams represent the actual implemented system architecture.
