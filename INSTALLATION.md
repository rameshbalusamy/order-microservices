# Installation Guide

## 📥 Download & Extract

### Option 1: ZIP File (Windows/Mac)
```bash
# Download order-microservices-saga.zip
unzip order-microservices-saga.zip
cd order-microservices-saga
```

### Option 2: TAR.GZ File (Linux)
```bash
# Download order-microservices-saga.tar.gz
tar -xzf order-microservices-saga.tar.gz
cd order-microservices-saga
```

## ✅ Verify Installation

Run the verification script:
```bash
bash setup-check.sh
```

This will check all files are present and show you:
- ✓ Complete file structure
- ✓ Total Java files (should be 40+)
- ✓ All documentation
- ✓ Quick start instructions

## 🔧 Prerequisites

### Required Software

1. **Docker** (20.10+)
   ```bash
   docker --version
   ```

2. **Docker Compose** (2.0+)
   ```bash
   docker-compose --version
   ```

### System Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 5GB free
- **CPU**: 4+ cores recommended

### Port Availability

Ensure these ports are free:
- `8080` - Order Service
- `8081` - Payment Service
- `8082` - Inventory Service
- `8083` - Notification Service
- `9090` - Prometheus
- `3000` - Grafana
- `16686` - Jaeger
- `5432` - PostgreSQL Order
- `5433` - PostgreSQL Payment
- `5434` - PostgreSQL Inventory
- `9092` - Kafka
- `2181` - Zookeeper

Check ports:
```bash
# Linux/Mac
lsof -i :8080
lsof -i :9092

# Windows
netstat -ano | findstr :8080
```

## 🚀 Start the System

### Step 1: Navigate to Project
```bash
cd order-microservices-saga
```

### Step 2: Start All Services
```bash
docker-compose up --build
```

**First-time build takes ~3-5 minutes**

### Step 3: Run in Background (Optional)
```bash
docker-compose up --build -d
```

### Step 4: Monitor Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f order-service
docker-compose logs -f payment-service
```

## ⏱️ Wait for Startup

Services take ~2-3 minutes to fully start. You'll see:

```
order-service       | Started OrderServiceApplication
payment-service     | Started PaymentServiceApplication
inventory-service   | Started InventoryServiceApplication
notification-service| Started NotificationServiceApplication
```

## ✅ Verify Services are Running

### Health Checks
```bash
# Order Service
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}

# Payment Service
curl http://localhost:8081/actuator/health

# Inventory Service
curl http://localhost:8082/actuator/health

# Notification Service
curl http://localhost:8083/actuator/health
```

### Check Docker Status
```bash
docker-compose ps
```

All services should show `Up` status.

## 🧪 Test the System

### Create Your First Order

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
  "customerId": "CUST-001",
  "status": "PENDING",
  "totalAmount": 299.99,
  "createdAt": "2026-02-10T...",
  ...
}
```

**Save the `orderId` from response!**

### Stream Real-Time Updates

Replace `ORD-XXXXXXXX` with your actual order ID:

```bash
curl -N http://localhost:8080/api/orders/ORD-XXXXXXXX/stream
```

**You'll see live updates:**
```
data: Connected. Current status: PAYMENT_PROCESSING
data: Payment completed successfully
data: Inventory reserved successfully
data: Order completed successfully!
```

### Check Final Status

```bash
curl http://localhost:8080/api/orders/ORD-XXXXXXXX
```

Status should be `COMPLETED`.

## 📊 Access Monitoring Tools

### Grafana (Dashboards)
1. Open: http://localhost:3000
2. Login: `admin` / `admin`
3. Skip password change (for demo)
4. Explore dashboards

### Jaeger (Distributed Tracing)
1. Open: http://localhost:16686
2. Select "order-service" from dropdown
3. Click "Find Traces"
4. Click any trace to see full flow

### Prometheus (Metrics)
1. Open: http://localhost:9090
2. Try queries:
   ```
   rate(http_server_requests_seconds_count[5m])
   ```

## 📝 Import Postman Collection

1. Open Postman
2. Click **Import**
3. Select `postman-collection.json`
4. Collection "Order Microservices SAGA" appears
5. Run folder "Scenario 1: Happy Path"

## 🛠️ Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs order-service

# Restart specific service
docker-compose restart order-service

# Rebuild service
docker-compose up --build order-service
```

### Port Already in Use

```bash
# Linux/Mac: Find process using port
lsof -i :8080
kill -9 <PID>

# Or change port in docker-compose.yml
# Change "8080:8080" to "8090:8080"
```

### Out of Memory

Increase Docker memory:
- Docker Desktop → Settings → Resources
- Set to at least 8GB

### Database Connection Issues

```bash
# Restart databases
docker-compose restart postgres-order postgres-payment postgres-inventory

# Or reset everything
docker-compose down -v
docker-compose up --build
```

### Kafka Connection Issues

```bash
# Restart Kafka
docker-compose restart kafka zookeeper

# Wait 30 seconds then restart services
sleep 30
docker-compose restart order-service payment-service inventory-service notification-service
```

## 🔄 Common Commands

### Stop Everything
```bash
docker-compose down
```

### Stop and Remove Data
```bash
docker-compose down -v
```

### View Service Logs
```bash
docker-compose logs -f <service-name>
```

### Restart Service
```bash
docker-compose restart <service-name>
```

### Rebuild Service
```bash
docker-compose up --build <service-name>
```

## 📚 Next Steps

After successful installation:

1. **Read Documentation**
   - `QUICKSTART.md` - Fast guide
   - `README.md` - Complete guide
   - `SHOW_AND_TELL.md` - Architecture details

2. **Run Test Scenarios**
   - Use Postman collection
   - Test payment failures
   - Test compensation flow

3. **Explore Monitoring**
   - Create custom Grafana dashboards
   - Explore Jaeger traces
   - Query Prometheus metrics

4. **Customize**
   - Modify failure rates
   - Add new endpoints
   - Create new services

## ✅ Installation Checklist

- [ ] Docker & Docker Compose installed
- [ ] Ports 8080-8083, 9090, 3000, 16686, 5432-5434, 9092, 2181 available
- [ ] 8GB+ RAM allocated to Docker
- [ ] Project extracted
- [ ] `setup-check.sh` runs successfully
- [ ] `docker-compose up` completes
- [ ] All 4 services show `{"status":"UP"}`
- [ ] Test order creates successfully
- [ ] SSE stream works
- [ ] Grafana accessible
- [ ] Jaeger shows traces
- [ ] Postman collection imported

## 🆘 Getting Help

If you encounter issues:

1. Run `bash setup-check.sh` to verify files
2. Check `docker-compose logs -f`
3. Verify ports with `netstat` or `lsof`
4. Review `QUICKSTART.md` and `README.md`
5. Check Docker resource allocation

## 🎉 Success!

If all health checks pass and you can create orders, you're ready to go!

Explore the SAGA pattern, test failure scenarios, and dive into the observability stack.

---

**Happy Coding! 🚀**
