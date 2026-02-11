# Quick SAGA Negative Testing Guide

## 🎯 Problem Solved

**Issue**: With default settings (10% payment failure, 5% inventory failure), it's hard to reliably test compensation.

**Solution**: Use testing configurations that **force** specific failure scenarios!

---

## 🚀 Quick Test Methods

### Method 1: Docker Compose Overrides (Recommended)

#### Test SAGA Compensation (Inventory Failure)

```bash
# Stop current services
docker-compose down -v

# Start with compensation testing configuration
docker-compose -f docker-compose.yml -f docker-compose.compensation-test.yml up --build

# This sets:
# - Payment: 0% failure (always succeeds)
# - Inventory: 50% failure (triggers compensation often!)
```

**Create a few orders:**
```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"COMP-TEST-$i\",
      \"customerEmail\": \"test$i@example.com\",
      \"totalAmount\": 299.99,
      \"items\": [{
        \"productId\": \"PROD-$i\",
        \"productName\": \"Test Product\",
        \"quantity\": 1,
        \"price\": 299.99
      }]
    }"
  sleep 2
done
```

**Expected**: ~2-3 orders will fail at inventory and trigger payment refund!

**Watch the compensation happen:**
```bash
docker-compose logs -f | grep -E "Inventory.*failed|Refund|refund|CANCELLED"
```

**You'll see:**
```
inventory-service | Simulating inventory failure (triggered by 50% failure rate)
order-service     | Inventory reservation failed. Initiating payment refund
order-service     | Refund command sent for order: ORD-XXXXXXXX
payment-service   | Refunding payment for order: ORD-XXXXXXXX
payment-service   | Payment refunded successfully
order-service     | Order status updated to: CANCELLED
```

---

#### Test Payment Failure (No Compensation)

```bash
# Stop current services
docker-compose down -v

# Start with payment failure testing configuration
docker-compose -f docker-compose.yml -f docker-compose.payment-failure-test.yml up --build

# This sets:
# - Payment: 50% failure
# - Inventory: 0% failure (never reached for failed payments)
```

**Create orders:**
```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"PAY-TEST-$i\",
      \"customerEmail\": \"test$i@example.com\",
      \"totalAmount\": 199.99,
      \"items\": [{
        \"productId\": \"PROD-$i\",
        \"productName\": \"Test Product\",
        \"quantity\": 1,
        \"price\": 199.99
      }]
    }"
  sleep 2
done
```

**Expected**: ~2-3 orders will fail at payment (no compensation!)

**Verify NO refunds:**
```bash
docker-compose logs payment-service | grep -i "failed"
docker-compose logs order-service | grep -i "refund" || echo "✅ Correct - no refunds for payment failures"
```

---

### Method 2: Environment Variables (Quick Override)

You can also set environment variables directly:

```bash
# Test compensation
docker-compose down -v
PAYMENT_FAILURE_RATE=0 INVENTORY_FAILURE_RATE=50 docker-compose up --build
```

Or edit `docker-compose.yml` directly and add under each service:

```yaml
payment-service:
  environment:
    - PAYMENT_FAILURE_RATE=0  # For testing compensation

inventory-service:
  environment:
    - INVENTORY_FAILURE_RATE=50  # For testing compensation
```

---

## 📊 Testing Scenarios Summary

| Configuration | Payment Rate | Inventory Rate | Purpose |
|--------------|--------------|----------------|---------|
| **Default** | 10% fail | 5% fail | Normal operations |
| **Compensation Test** | 0% fail | 50% fail | **Force SAGA compensation** |
| **Payment Failure Test** | 50% fail | 0% fail | Test payment failures |
| **High Failure** | 30% fail | 30% fail | Stress testing |

---

## ✅ Success Verification

### For Compensation Testing

**Create 5 orders, check one that failed:**

```bash
# Get order ID from response
ORDER_ID="ORD-XXXXXXXX"

# Check status
curl http://localhost:8080/api/orders/$ORDER_ID | jq '{orderId, status}'

# Should show: "status": "CANCELLED"

# Verify compensation in logs
docker-compose logs order-service payment-service | grep $ORDER_ID | grep -E "Refund|refund"

# Should see:
# - "Refund command sent"
# - "Payment refunded successfully"
```

### For Payment Failure Testing

**Check that NO refunds occur:**

```bash
# This should return nothing (or "Correct - no refunds...")
docker-compose logs order-service | grep -i "refund" || echo "✅ Correct - no refunds for payment failures"
```

---

## 🎬 Complete Test Session Example

```bash
# 1. Clean start
docker-compose down -v

# 2. Start with compensation testing
docker-compose -f docker-compose.yml -f docker-compose.compensation-test.yml up --build

# 3. Wait for services (2-3 minutes)
# Watch for: "Started OrderServiceApplication"

# 4. In another terminal, create orders
for i in {1..10}; do
  curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"TEST-$i\",
      \"customerEmail\": \"test$i@example.com\",
      \"totalAmount\": 299.99,
      \"items\": [{
        \"productId\": \"PROD-$i\",
        \"productName\": \"Product $i\",
        \"quantity\": 1,
        \"price\": 299.99
      }]
    }" | jq -r '.orderId'
  sleep 3
done

# 5. Watch for compensation events
docker-compose logs -f | grep -E "Inventory.*failed|Refund|refund"

# 6. You'll see multiple compensation flows!
```

**Expected Output:**
```
inventory-service | Inventory reservation failed (50% failure rate)
order-service     | Initiating payment refund
payment-service   | Refunding payment: PAY-ABC123
payment-service   | Payment refunded successfully
order-service     | Order ORD-XYZ789 status updated to: CANCELLED
```

---

## 🔍 Debugging Tips

### No Failures Showing Up?

```bash
# Check if environment variables are set
docker-compose -f docker-compose.yml -f docker-compose.compensation-test.yml config | grep FAILURE_RATE

# Should show:
# PAYMENT_FAILURE_RATE=0
# INVENTORY_FAILURE_RATE=50
```

### Services Not Starting?

```bash
# Check logs
docker-compose logs payment-service | tail -50
docker-compose logs inventory-service | tail -50
```

### Want to See All Order Statuses?

```bash
# Watch order statuses in real-time
watch -n 2 'docker-compose logs order-service | grep "Order.*status updated" | tail -10'
```

---

## 📝 Files Included

- `docker-compose.compensation-test.yml` - Forces inventory failures
- `docker-compose.payment-failure-test.yml` - Forces payment failures
- `TESTING_CONFIG.md` - Detailed configuration guide
- `test-saga-compensation.sh` - Automated testing script

---

## 🎓 Key Takeaways

1. **Default config**: Hard to test (only 5-10% failures)
2. **Testing configs**: Force specific scenarios for reliable testing
3. **Compensation test**: Payment succeeds, inventory fails → refund happens
4. **Payment failure test**: Payment fails → no refund needed

---

**Ready to test? Use the compensation testing configuration and see SAGA compensation in action!**

```bash
docker-compose down -v
docker-compose -f docker-compose.yml -f docker-compose.compensation-test.yml up --build
```
