# SAGA Pattern Testing Guide

## 🚀 Quick Start - Automated Testing

**Want to see SAGA compensation in action right now?**

```bash
# Run the automated test script
bash test-saga-compensation.sh
```

This script will:
1. ✅ Create 10 orders to test payment failures (no compensation)
2. ✅ Create 20 orders to test inventory failures (WITH compensation)
3. ✅ Automatically detect and report compensation events
4. ✅ Show you exactly what happened

**Expected output**:
- ~1 payment failure (no compensation needed)
- ~1 inventory failure (compensation triggered - payment refunded!)

---

## Overview

This guide covers comprehensive testing of the SAGA pattern implementation, including **positive scenarios** (happy path) and **negative scenarios** (failure and compensation).

---

## 🎯 Testing Scenarios

### ⚠️ IMPORTANT: Understanding SAGA Compensation

**SAGA Compensation only happens when a LATER step fails AFTER earlier steps succeeded.**

| Scenario | Payment | Inventory | Compensation? | Why? |
|----------|---------|-----------|---------------|------|
| **Payment Fails** | ❌ FAILED | 🔘 Never Started | ❌ **NO** | Nothing to rollback - inventory never reserved |
| **Inventory Fails** | ✅ SUCCESS | ❌ FAILED | ✅ **YES** | Must refund payment! |

---

### Scenario 1: Happy Path ✅
**Description**: All services succeed, order completes successfully

**Flow**:
```
Order Created → Payment Success → Inventory Reserved → Notification Sent → COMPLETED
```

**Expected Results**:
- ✅ Order status: `COMPLETED`
- ✅ Payment status: `COMPLETED`
- ✅ Inventory status: `RESERVED`
- ✅ Notification sent
- ✅ No compensating transactions

---

### Scenario 2: Payment Failure ❌
**Description**: Payment service fails, order is cancelled

**Flow**:
```
Order Created → Payment Failed → Order CANCELLED
```

**Expected Results**:
- ✅ Order status: `PAYMENT_FAILED` → `CANCELLED`
- ✅ Payment status: `FAILED`
- ✅ **No compensation needed** (inventory was never reserved)
- ✅ No notification sent

**Key Point**: Since payment failed BEFORE inventory reservation, there's nothing to compensate.

---

### Scenario 3: Inventory Failure with Compensation 🔄
**Description**: Payment succeeds but inventory fails, triggering SAGA compensation

**Flow**:
```
Order Created → Payment Success → Inventory Failed → 
🔄 COMPENSATION: Refund Payment → Order CANCELLED
```

**Expected Results**:
- ✅ Order status: `INVENTORY_FAILED` → `CANCELLED`
- ✅ Payment status: `COMPLETED` → `REFUNDED`
- ✅ Inventory status: `FAILED`
- ✅ **Compensation triggered**: Payment refund command sent
- ✅ No notification sent (order cancelled)

**Key Point**: This is the **true SAGA compensation** scenario - a previous successful step (payment) must be rolled back.

---

## 📝 Test Execution

### Method 1: Using Postman Collection

#### Test 1: Happy Path
```bash
# Import postman-collection.json
# Run: "Scenario 1: Happy Path"
# Expected: ~90% success rate
```

**Verification**:
```bash
# Check order status
curl http://localhost:8080/api/orders/{orderId}

# Should show: "status": "COMPLETED"
```

#### Test 2: Payment Failure
```bash
# In Postman: Run "Scenario 3: Payment Failure" folder
# Create 10 orders - approximately 1 will fail
```

**What to Look For**:
```json
{
  "orderId": "ORD-XXXXXXXX",
  "status": "CANCELLED"
}
```

**Verify in Logs**:
```bash
docker-compose logs payment-service | grep "Payment failed"
docker-compose logs order-service | grep "CANCELLED"
```

#### Test 3: Inventory Failure with Compensation
```bash
# In Postman: Run "Scenario 4: Inventory Failure" folder
# Create 20 orders - approximately 1 will fail at inventory
```

**What to Look For**:
```json
{
  "orderId": "ORD-XXXXXXXX",
  "status": "CANCELLED"
}
```

**Verify Compensation in Logs**:
```bash
# Watch for the compensation sequence
docker-compose logs -f order-service inventory-service payment-service | grep -E "Inventory failed|Refund|CANCELLED"
```

**Expected Log Sequence**:
```
inventory-service | Inventory reservation failed for order: ORD-XXXXXXXX
order-service     | Inventory reservation failed. Initiating payment refund
order-service     | Refund command sent for order: ORD-XXXXXXXX
payment-service   | Refunding payment: PAY-YYYYYYYY for order: ORD-XXXXXXXX
payment-service   | Payment refunded successfully
order-service     | Order status updated to: CANCELLED
```

---

### Method 2: Manual cURL Testing

#### Create Orders and Monitor

```bash
# Terminal 1: Watch logs
docker-compose logs -f order-service payment-service inventory-service

# Terminal 2: Create orders
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"TEST-$i\",
      \"customerEmail\": \"test$i@example.com\",
      \"totalAmount\": 299.99,
      \"items\": [{
        \"productId\": \"PROD-$i\",
        \"productName\": \"Test Product $i\",
        \"quantity\": 1,
        \"price\": 299.99
      }]
    }" | jq '.orderId'
  
  sleep 1
done
```

**Statistics to Expect**:
- ~18 orders: `COMPLETED` (90% payment success × ~95% inventory success)
- ~2 orders: `CANCELLED` with `PAYMENT_FAILED` (10% payment failure)
- ~1 order: `CANCELLED` with `INVENTORY_FAILED` + payment refund (5% inventory failure)

---

## 🔍 Detailed Verification Steps

### Verify Payment Failure (No Compensation)

```bash
# 1. Create order
ORDER_ID=$(curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{...}' | jq -r '.orderId')

# 2. Wait 3 seconds
sleep 3

# 3. Check status
curl http://localhost:8080/api/orders/$ORDER_ID | jq '{orderId, status}'

# 4. If status is CANCELLED, check logs
docker-compose logs payment-service | grep $ORDER_ID | grep "failed"
docker-compose logs order-service | grep $ORDER_ID | grep "PAYMENT_FAILED"

# 5. Verify NO refund command was sent (since nothing to compensate)
docker-compose logs order-service | grep $ORDER_ID | grep "Refund" || echo "✅ No refund - correct behavior!"
```

### Verify Inventory Failure with Compensation

```bash
# 1. Create orders until one fails at inventory
for i in {1..25}; do
  ORDER_ID=$(curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"COMP-TEST-$i\",
      \"customerEmail\": \"comp$i@test.com\",
      \"totalAmount\": 499.99,
      \"items\": [{
        \"productId\": \"PROD-COMP-$i\",
        \"productName\": \"Compensation Test $i\",
        \"quantity\": 1,
        \"price\": 499.99
      }]
    }" | jq -r '.orderId')
  
  echo "Created: $ORDER_ID"
  
  # Wait for processing
  sleep 4
  
  # Check if it failed at inventory
  STATUS=$(curl -s http://localhost:8080/api/orders/$ORDER_ID | jq -r '.status')
  
  if [ "$STATUS" == "CANCELLED" ]; then
    echo "🔍 Found cancelled order: $ORDER_ID"
    
    # Check if it was inventory failure
    docker-compose logs inventory-service | grep $ORDER_ID | grep -q "failed"
    if [ $? -eq 0 ]; then
      echo "✅ Inventory failure detected"
      
      # Verify compensation
      docker-compose logs order-service | grep $ORDER_ID | grep "Refund"
      docker-compose logs payment-service | grep $ORDER_ID | grep "refunded"
      
      echo ""
      echo "=== SAGA COMPENSATION VERIFIED ==="
      break
    fi
  fi
done
```

---

## 📊 Expected Statistics (100 Orders)

| Scenario | Count | Percentage | Status |
|----------|-------|------------|--------|
| **Success** | ~85-90 | ~85-90% | COMPLETED |
| **Payment Failure** | ~10 | ~10% | CANCELLED (no compensation) |
| **Inventory Failure** | ~5 | ~5% | CANCELLED (WITH compensation) |

---

## 🔬 Deep Dive: SAGA Compensation Flow

### What Happens When Inventory Fails

**Step-by-Step**:

1. **Order Created** - Status: `PENDING`
   ```
   Order Service saves order to database
   ```

2. **Payment Processing** - Status: `PAYMENT_PROCESSING`
   ```
   Order Service publishes: order-created
   Payment Service receives event
   Payment Service processes payment (1 second delay)
   ```

3. **Payment Success** - Status: `PAYMENT_COMPLETED`
   ```
   Payment Service publishes: payment-completed
   Order Service receives event
   Order Service updates status
   ```

4. **Inventory Reservation** - Status: `INVENTORY_RESERVING`
   ```
   Inventory Service receives: payment-completed
   Inventory Service attempts reservation
   ```

5. **Inventory Fails** - Status: `INVENTORY_FAILED`
   ```
   Inventory Service publishes: inventory-failed
   Order Service receives event
   ```

6. **🔄 COMPENSATION STARTS**
   ```
   Order Service checks: Was payment completed? YES
   Order Service publishes: refund-payment command
   ```

7. **Payment Refund** - Status stays `INVENTORY_FAILED`
   ```
   Payment Service receives: refund-payment
   Payment Service refunds the payment
   Payment Service publishes: payment-refunded
   ```

8. **Order Cancelled** - Status: `CANCELLED`
   ```
   Order Service receives: payment-refunded
   Order Service updates status to CANCELLED
   ```

### Logs to Watch

```bash
# Watch the complete SAGA compensation
docker-compose logs -f | grep -E "ORD-[A-Z0-9]+" | grep -E "failed|Refund|refund|CANCELLED"
```

**Example Output**:
```
inventory-service | 2026-02-11 02:00:15 - Inventory reservation failed for order: ORD-ABC12345. Reason: Insufficient stock
order-service     | 2026-02-11 02:00:15 - Inventory reservation failed for order: ORD-ABC12345
order-service     | 2026-02-11 02:00:15 - Initiating payment refund for order: ORD-ABC12345
order-service     | 2026-02-11 02:00:15 - Refund command sent for order: ORD-ABC12345
payment-service   | 2026-02-11 02:00:15 - Refunding payment: PAY-XYZ789 for order: ORD-ABC12345
payment-service   | 2026-02-11 02:00:15 - Payment refunded successfully for order: ORD-ABC12345
order-service     | 2026-02-11 02:00:15 - Order ORD-ABC12345 status updated to: CANCELLED
```

---

## 🎓 Understanding the Difference

### Payment Failure (No Compensation)
```
✅ Payment fails BEFORE inventory reservation
❌ No compensation needed
💡 Simple failure, just cancel the order
```

### Inventory Failure (WITH Compensation)
```
✅ Payment succeeds
✅ Inventory fails AFTER payment
🔄 Compensation required: Must refund payment
💡 This is the SAGA pattern in action!
```

---

## 📸 Screenshots to Capture

### 1. Happy Path
- Postman request showing order creation
- Order status showing `COMPLETED`
- Logs showing all services processed successfully

### 2. Payment Failure
- Postman request showing multiple order creation
- One order with status `CANCELLED` and `PAYMENT_FAILED`
- Logs showing payment failure
- Logs showing NO refund command

### 3. Inventory Failure with Compensation
- Postman request showing multiple order creation
- One order with status `CANCELLED` and `INVENTORY_FAILED`
- Logs showing:
  - Inventory failure
  - Refund command sent
  - Payment refunded
  - Order cancelled
- This proves SAGA compensation worked!

### 4. Jaeger Traces
- Successful order trace showing all services
- Failed order trace showing compensation flow
- Screenshot highlighting the refund-payment span

### 5. Grafana Metrics
- Payment success/failure rates
- Compensation transaction count
- Order status distribution

---

## ✅ Success Criteria

Your SAGA implementation is working correctly if:

1. ✅ **90% of orders complete successfully**
2. ✅ **~10% fail at payment** (status: CANCELLED, no compensation)
3. ✅ **~5% fail at inventory** (status: CANCELLED, WITH compensation)
4. ✅ **Compensation logs show**:
   - "Refund command sent"
   - "Payment refunded successfully"
   - Final status is CANCELLED
5. ✅ **Jaeger traces show compensation flow**
6. ✅ **No stuck orders** (all reach either COMPLETED or CANCELLED)

---

## 🐛 Common Issues

### Issue: No Orders Fail
**Problem**: Not enough orders created to trigger failures  
**Solution**: Create 20-50 orders to see statistically significant failures

### Issue: Orders Stuck in INVENTORY_RESERVING
**Problem**: Inventory service not processing events  
**Solution**: Check `docker-compose logs inventory-service`

### Issue: Refund Not Happening
**Problem**: Order service not detecting inventory failure  
**Solution**: Check if `inventory-failed` topic has messages:
```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic inventory-failed --from-beginning
```

---

## 📚 Additional Resources

- **README.md** - Architecture overview
- **SHOW_AND_TELL.md** - Detailed flow diagrams
- **TROUBLESHOOTING.md** - Common issues and fixes

---

**Remember**: The SAGA pattern is about handling failures gracefully. The compensation flow (payment refund when inventory fails) is the **key feature** to test and demonstrate!
