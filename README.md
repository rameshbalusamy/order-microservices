# Order Microservices with SAGA Pattern

A comprehensive microservices-based order management system demonstrating the SAGA pattern for distributed transactions, built with Spring Boot, Kafka, and complete observability stack.

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Order Management System                      │
│                        (SAGA Pattern Implementation)                 │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│    Client    │         │   Grafana    │         │   Jaeger     │
│  (Postman)   │───────► │  Dashboard   │         │   Tracing    │
└──────┬───────┘         └──────────────┘         └──────────────┘
       │                                                   ▲
       │                                                   │
       ▼                                                   │
┌─────────────────────────────────────────────────────────┼──────────┐
│  ORDER SERVICE (Port 8080) - SAGA Orchestrator          │          │
│  ┌────────────────────────────────────────────┐         │          │
│  │  • Receives order creation requests        │         │          │
│  │  • Orchestrates SAGA workflow              │         │          │
│  │  • Maintains order state                   │         │          │
│  │  • Streams real-time updates (SSE)         │         │          │
│  │  • Handles compensating transactions       │         │          │
│  └────────────────────────────────────────────┘         │          │
└───────────────┬─────────────────────────────────────────┼──────────┘
                │                                          │
                │          Apache Kafka Event Bus          │
                │    (Topics: order-created, payment-*,    │
                │     inventory-*, notification-*, etc.)   │
                │                                          │
    ┌───────────┼──────────────────────────────┬──────────┼─────┐
    │           │                              │          │     │
    ▼           ▼                              ▼          ▼     │
┌────────────────────┐  ┌──────────────────┐  ┌────────────────┴──┐
│ PAYMENT SERVICE    │  │ INVENTORY SERVICE │  │ NOTIFICATION      │
│   (Port 8081)      │  │   (Port 8082)     │  │   SERVICE         │
│                    │  │                   │  │   (Port 8083)     │
│ • Process payments │  │ • Reserve stock   │  │ • Email customers │
│ • Refund on        │  │ • Release on      │  │ • Send updates    │
│   failure (SAGA    │  │   failure (SAGA   │  │ • Event logging   │
│   compensation)    │  │   compensation)   │  │                   │
│                    │  │                   │  │                   │
│ PostgreSQL DB      │  │ PostgreSQL DB     │  │ (Stateless)       │
└────────────────────┘  └───────────────────┘  └───────────────────┘
         │                       │
         └───────────────┬───────┘
                         │
                         ▼
              ┌───────────────────┐
              │   Prometheus      │
              │   Metrics         │
              └───────────────────┘
```

## 🎯 Key Features

### 1. **SAGA Pattern Implementation**
- **Orchestration-based SAGA**: Order Service acts as the orchestrator
- **Compensating Transactions**: Automatic rollback on failures
  - Payment refund if inventory reservation fails
  - Order cancellation on any step failure
- **Event-Driven Architecture**: All services communicate via Kafka

### 2. **Real-Time Status Streaming**
- **Server-Sent Events (SSE)**: Real-time order status updates
- **WebSocket Alternative**: Lightweight SSE for status broadcasting
- **Progressive Updates**: Track order through each stage

### 3. **Complete Observability**
- **Distributed Tracing**: Jaeger integration for end-to-end request tracking
- **Metrics**: Prometheus + Grafana dashboards
- **Logging**: Structured logging with correlation IDs
- **Health Checks**: Actuator endpoints for all services

### 4. **API-First Design**
- **OpenAPI 3.0 Specification**: Complete API contract
- **Code Generation**: Delegate pattern for clean separation
- **Swagger UI**: Interactive API documentation

## 📋 Prerequisites

- Docker & Docker Compose (v2.0+)
- Java 17+ (for local development)
- Maven 3.8+ (for local development)
- Postman (for testing)
- 8GB RAM minimum for running all services

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd order-microservices-saga
```

### 2. Start All Services
```bash
docker-compose up --build
```

This command will:
- Start Zookeeper & Kafka
- Start 3 PostgreSQL databases
- Start Prometheus, Grafana, and Jaeger
- Build and start all 4 microservices

### 3. Wait for Services to be Ready
All services should be up in ~2-3 minutes. Check health:

```bash
# Check all services
curl http://localhost:8080/actuator/health  # Order Service
curl http://localhost:8081/actuator/health  # Payment Service
curl http://localhost:8082/actuator/health  # Inventory Service
curl http://localhost:8083/actuator/health  # Notification Service
```

### 4. Access Monitoring Tools

| Tool | URL | Credentials |
|------|-----|-------------|
| Grafana | http://localhost:3000 | admin/admin |
| Prometheus | http://localhost:9090 | - |
| Jaeger UI | http://localhost:16686 | - |

## 📝 API Usage

### Create an Order (POST /api/orders)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "customer@example.com",
    "totalAmount": 299.99,
    "items": [
      {
        "productId": "PROD-001",
        "productName": "Laptop",
        "quantity": 1,
        "price": 299.99
      }
    ]
  }'
```

**Response:**
```json
{
  "orderId": "ORD-ABC12345",
  "customerId": "CUST-001",
  "items": [...],
  "totalAmount": 299.99,
  "status": "PENDING",
  "createdAt": "2026-02-10T10:30:00",
  "updatedAt": "2026-02-10T10:30:00"
}
```

### Get Order Status (GET /api/orders/{orderId})

```bash
curl http://localhost:8080/api/orders/ORD-ABC12345
```

### Stream Real-Time Order Updates (SSE)

```bash
curl -N http://localhost:8080/api/orders/ORD-ABC12345/stream
```

**Output:**
```
data: Connected. Current status: PAYMENT_PROCESSING
data: Payment completed successfully
data: Inventory reserved successfully
data: Order completed successfully!
```

## 🔄 SAGA Workflow

### Success Flow

```
1. Order Created (PENDING)
   ↓
2. Payment Processing (PAYMENT_PROCESSING)
   ↓
3. Payment Completed (PAYMENT_COMPLETED)
   ↓
4. Inventory Reserving (INVENTORY_RESERVING)
   ↓
5. Inventory Reserved (INVENTORY_RESERVED)
   ↓
6. Notifying (NOTIFYING)
   ↓
7. Order Completed (COMPLETED)
```

### Failure Flow with Compensation

```
1. Order Created (PENDING)
   ↓
2. Payment Processing (PAYMENT_PROCESSING)
   ↓
3. Payment Completed (PAYMENT_COMPLETED)
   ↓
4. Inventory Reserving (INVENTORY_RESERVING)
   ↓
5. Inventory Failed (INVENTORY_FAILED)
   ↓
6. COMPENSATION: Refund Payment
   ↓
7. Order Cancelled (CANCELLED)
```

## 🧪 Testing Scenarios

### Scenario 1: Successful Order
- All services healthy
- Payment succeeds (90% probability)
- Inventory available
- Notification sent
- **Expected**: Order status = COMPLETED

### Scenario 2: Payment Failure
- Payment service simulates failure (10% probability)
- **Expected**: Order status = PAYMENT_FAILED → CANCELLED
- No inventory reservation attempted

### Scenario 3: Inventory Failure
- Payment succeeds
- Inventory service fails (manually simulate)
- **Expected**: 
  - Order status = INVENTORY_FAILED
  - Payment refunded automatically
  - Order status = CANCELLED

### Scenario 4: Real-Time Tracking
- Create order
- Immediately connect to SSE endpoint
- **Expected**: Receive real-time updates for each status change

## 📊 Monitoring & Observability

### Grafana Dashboards

1. **Microservices Overview**
   - Request rates for all services
   - Error rates
   - Response times (p50, p95, p99)

2. **SAGA Flow Metrics**
   - Order creation rate
   - Payment success/failure ratio
   - Inventory reservation rate
   - Compensation transaction rate

3. **Kafka Metrics**
   - Topic throughput
   - Consumer lag
   - Message processing time

### Jaeger Tracing

View distributed traces:
1. Open http://localhost:16686
2. Select "order-service" from dropdown
3. Search for traces
4. See complete request flow across all services

**Trace Example:**
```
order-service: POST /api/orders (200ms)
  ├─ kafka: publish order-created (5ms)
  ├─ payment-service: process payment (1000ms)
  │   └─ kafka: publish payment-completed (5ms)
  ├─ inventory-service: reserve inventory (500ms)
  │   └─ kafka: publish inventory-reserved (5ms)
  └─ notification-service: send notification (100ms)
```

### Prometheus Queries

```promql
# Order creation rate
rate(http_server_requests_seconds_count{uri="/api/orders",method="POST"}[5m])

# Payment success rate
rate(payment_processed_total{status="success"}[5m])

# SAGA compensation rate
rate(payment_refunded_total[5m])
```

## 🛠️ Development

### Project Structure

```
order-microservices-saga/
├── api-spec.yaml                 # OpenAPI specification
├── docker-compose.yml            # Infrastructure setup
├── monitoring/
│   ├── prometheus.yml
│   └── grafana/
│       ├── dashboards/
│       └── datasources/
├── order-service/               # SAGA Orchestrator
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/orderms/order/
│           │       ├── controller/
│           │       ├── service/
│           │       ├── repository/
│           │       ├── model/
│           │       └── kafka/
│           └── resources/
│               └── application.yml
├── payment-service/            # Payment processing
├── inventory-service/          # Inventory management
└── notification-service/       # Customer notifications
```

### Building Locally

```bash
# Build all services
cd order-service && mvn clean install
cd ../payment-service && mvn clean install
cd ../inventory-service && mvn clean install
cd ../notification-service && mvn clean install
```

### Running Locally (Without Docker)

1. Start infrastructure:
```bash
docker-compose up zookeeper kafka postgres-order postgres-payment postgres-inventory prometheus grafana jaeger
```

2. Run each service:
```bash
# Terminal 1
cd order-service && mvn spring-boot:run

# Terminal 2
cd payment-service && mvn spring-boot:run

# Terminal 3
cd inventory-service && mvn spring-boot:run

# Terminal 4
cd notification-service && mvn spring-boot:run
```

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| SPRING_PROFILES_ACTIVE | default | Use 'docker' for containerized deployment |
| KAFKA_BOOTSTRAP_SERVERS | localhost:29092 | Kafka broker addresses |
| DB_URL | localhost:5432 | PostgreSQL host |
| JAEGER_ENDPOINT | localhost:9411 | Jaeger collector endpoint |

### Kafka Topics

| Topic | Producer | Consumer | Description |
|-------|----------|----------|-------------|
| order-created | Order Service | Payment Service | Triggers payment |
| payment-completed | Payment Service | Order Service, Inventory Service | Payment success |
| payment-failed | Payment Service | Order Service | Payment failure |
| inventory-reserved | Inventory Service | Order Service, Notification Service | Stock reserved |
| inventory-failed | Inventory Service | Order Service | Stock unavailable |
| refund-payment | Order Service | Payment Service | Compensation trigger |
| payment-refunded | Payment Service | Order Service | Refund completed |
| notification-sent | Notification Service | Order Service | Notification delivered |

## 🐛 Troubleshooting

### Services Not Starting

```bash
# Check Docker logs
docker-compose logs -f order-service
docker-compose logs -f payment-service

# Restart specific service
docker-compose restart order-service
```

### Kafka Connection Issues

```bash
# Verify Kafka is running
docker-compose ps kafka

# Check Kafka topics
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Database Issues

```bash
# Access database
docker exec -it postgres-order psql -U orderuser -d orderdb

# Check tables
\dt
```

## 📚 API Documentation

Full API documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: [api-spec.yaml](./api-spec.yaml)

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License.

## 🙏 Acknowledgments

- Spring Boot Team
- Apache Kafka
- Jaeger Tracing
- Prometheus & Grafana Community

## 📞 Support

For issues and questions:
- Create an issue in GitHub
- Email: support@orderms.com

---

**Built with ❤️ using Spring Boot, Kafka, and Cloud-Native Patterns**
