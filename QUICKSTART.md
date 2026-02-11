# 🚀 Quick Start Guide

Get the Order Microservices SAGA system up and running in 5 minutes!

## Prerequisites Check

Before starting, ensure you have:
```bash
docker --version    # Should be 20.10+
docker-compose --version  # Should be 2.0+
```

## Step-by-Step Setup

### 1. Start the System (2-3 minutes)

```bash
# Navigate to project directory
cd order-microservices-saga

# Start all services
docker-compose up --build

# Alternative: Run in background
docker-compose up --build -d
```

**What's starting:**
- ✅ Zookeeper & Kafka
- ✅ 3x PostgreSQL databases
- ✅ 4x Microservices (Order, Payment, Inventory, Notification)
- ✅ Prometheus, Grafana, Jaeger

### 2. Verify Services (30 seconds)

Wait for all services to be healthy:

```bash
# Check service health
curl http://localhost:8080/actuator/health  # Order Service
curl http://localhost:8081/actuator/health  # Payment Service
curl http://localhost:8082/actuator/health  # Inventory Service
curl http://localhost:8083/actuator/health  # Notification Service

# All should return: {"status":"UP"}
```

### 3. Create Your First Order (10 seconds)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "test@example.com",
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

**Expected Response:**
```json
{
  "orderId": "ORD-XXXXXXXX",
  "status": "PENDING",
  "customerId": "CUST-001",
  ...
}
```

**Save the `orderId` from the response!**

### 4. Track Order in Real-Time (Live Updates)

```bash
# Replace ORD-XXXXXXXX with your actual order ID
curl -N http://localhost:8080/api/orders/ORD-XXXXXXXX/stream
```

**You'll see:**
```
data: Connected. Current status: PAYMENT_PROCESSING
data: Payment completed successfully
data: Inventory reserved successfully
data: Order completed successfully!
```

### 5. Check Final Order Status

```bash
curl http://localhost:8080/api/orders/ORD-XXXXXXXX
```

**Expected:**
```json
{
  "orderId": "ORD-XXXXXXXX",
  "status": "COMPLETED",
  ...
}
```

## 🎉 Success! What's Next?

### Access Monitoring Tools

| Tool | URL | Purpose |
|------|-----|---------|
| **Grafana** | http://localhost:3000 | Dashboards (admin/admin) |
| **Jaeger** | http://localhost:16686 | Distributed Tracing |
| **Prometheus** | http://localhost:9090 | Metrics |

### Use Postman Collection

1. Import `postman-collection.json` into Postman
2. Run the "Scenario 1: Happy Path" folder
3. See all requests execute automatically

### Explore SAGA Patterns

**Test Payment Failure:**
- Create multiple orders (10% will fail due to payment)
- Watch the automatic cancellation

**Test Compensation:**
- Orders may fail at inventory stage
- Watch automatic payment refund happen!

## Common Commands

```bash
# View logs
docker-compose logs -f order-service
docker-compose logs -f payment-service

# Restart a service
docker-compose restart order-service

# Stop everything
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Rebuild specific service
docker-compose up --build order-service
```

## 🐛 Troubleshooting

### Services Won't Start

```bash
# Check if ports are in use
lsof -i :8080  # Order Service
lsof -i :9092  # Kafka

# Kill processes using these ports or change ports in docker-compose.yml
```

### Kafka Connection Issues

```bash
# Restart Kafka and dependent services
docker-compose restart kafka
sleep 10
docker-compose restart order-service payment-service inventory-service notification-service
```

### Database Connection Issues

```bash
# Check database status
docker-compose ps postgres-order postgres-payment postgres-inventory

# Restart databases
docker-compose restart postgres-order postgres-payment postgres-inventory
```

### Health Checks Failing

```bash
# Wait a bit longer (services need ~2-3 minutes to fully start)
# Check individual service logs
docker-compose logs order-service | tail -50
```

## 📊 Quick Tests

### Test 1: Create 5 Orders Rapidly
```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"CUST-$i\",
      \"customerEmail\": \"test$i@example.com\",
      \"totalAmount\": 99.99,
      \"items\": [{
        \"productId\": \"PROD-$i\",
        \"productName\": \"Product $i\",
        \"quantity\": 1,
        \"price\": 99.99
      }]
    }" &
done
```

### Test 2: Monitor All Orders
```bash
# In Grafana (http://localhost:3000)
# 1. Login with admin/admin
# 2. Go to Dashboards → Microservices Overview
# 3. See real-time metrics
```

### Test 3: View Distributed Traces
```bash
# In Jaeger (http://localhost:16686)
# 1. Select "order-service" from dropdown
# 2. Click "Find Traces"
# 3. Click on any trace to see the full flow
```

## 🎯 Success Criteria

Your system is working correctly if:

✅ All health checks return `{"status":"UP"}`  
✅ Orders can be created (POST /api/orders returns 201)  
✅ Orders progress through statuses (PENDING → ... → COMPLETED)  
✅ SSE streaming shows real-time updates  
✅ ~90% of orders complete successfully  
✅ ~10% fail at payment (expected behavior)  
✅ Failed orders trigger compensation (refund)  
✅ Grafana shows service metrics  
✅ Jaeger shows distributed traces  

## Next Steps

1. **Read the full README.md** for detailed architecture
2. **Explore SHOW_AND_TELL.md** for complete documentation
3. **Import Postman collection** for easy testing
4. **Check Grafana dashboards** for insights
5. **View Jaeger traces** to understand the flow

---

**Need Help?** Check the troubleshooting section or create an issue on GitHub.

**Ready to dive deeper?** Read the [README.md](./README.md) and [SHOW_AND_TELL.md](./SHOW_AND_TELL.md)!
