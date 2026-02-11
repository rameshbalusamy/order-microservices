# SAGA Compensation Flow Comparison

## 🔴 Scenario 1: Payment Failure (NO Compensation)

```
┌─────────────┐
│Order Service│
└──────┬──────┘
       │ 1. Create Order
       │ Status: PENDING
       │
       │ 2. Publish: order-created
       ▼
┌──────────────┐
│Payment Service│
└──────┬───────┘
       │ 3. Process Payment
       │ ❌ PAYMENT FAILS
       │
       │ 4. Publish: payment-failed
       ▼
┌─────────────┐
│Order Service│
└──────┬──────┘
       │ 5. Receive: payment-failed
       │ 6. Update Status: CANCELLED
       │
       │ ❌ NO COMPENSATION NEEDED
       │ (Inventory was never touched)
       ▼
    [END]
```

**Key Points:**
- ❌ Payment failed FIRST
- 🔘 Inventory service never received an event
- 🔘 Nothing to roll back
- ✅ Simply cancel the order

---

## 🟢 Scenario 2: Inventory Failure (WITH Compensation)

```
┌─────────────┐
│Order Service│
└──────┬──────┘
       │ 1. Create Order
       │ Status: PENDING
       │
       │ 2. Publish: order-created
       ▼
┌──────────────┐
│Payment Service│
└──────┬───────┘
       │ 3. Process Payment
       │ ✅ PAYMENT SUCCEEDS
       │ Status: COMPLETED
       │
       │ 4. Publish: payment-completed
       ▼
┌─────────────┐
│Order Service│ ────┬──────────────────┐
└──────┬──────┘     │                  │
       │            ▼                  │
       │    ┌────────────────┐        │
       │    │Inventory Service│        │
       │    └────────┬───────┘        │
       │            │ 5. Reserve Inventory
       │            │ ❌ INVENTORY FAILS
       │            │                  │
       │            │ 6. Publish: inventory-failed
       │            ▼                  │
       │    ┌─────────────┐           │
       └───▶│Order Service│◀──────────┘
            └──────┬──────┘
                   │ 7. Receive: inventory-failed
                   │ 
                   │ 🔄 COMPENSATION STARTS
                   │ 8. Publish: refund-payment
                   ▼
            ┌──────────────┐
            │Payment Service│
            └──────┬───────┘
                   │ 9. Refund Payment
                   │ Status: REFUNDED
                   │
                   │ 10. Publish: payment-refunded
                   ▼
            ┌─────────────┐
            │Order Service│
            └──────┬──────┘
                   │ 11. Receive: payment-refunded
                   │ 12. Update Status: CANCELLED
                   ▼
                 [END]
```

**Key Points:**
- ✅ Payment succeeded FIRST
- ✅ Then inventory failed
- 🔄 Must compensate by refunding payment
- ✅ This is the SAGA pattern in action!

---

## 📊 Side-by-Side Comparison

| Step | Payment Failure | Inventory Failure |
|------|----------------|-------------------|
| **Order Created** | ✅ | ✅ |
| **Payment Processing** | ❌ FAILS | ✅ SUCCEEDS |
| **Inventory Reservation** | 🔘 Never happens | ❌ FAILS |
| **Compensation Needed?** | ❌ NO | ✅ YES |
| **Compensation Action** | None | Refund Payment |
| **Final Status** | CANCELLED | CANCELLED |
| **Database State** | Order: CANCELLED<br>Payment: FAILED | Order: CANCELLED<br>Payment: REFUNDED<br>Inventory: FAILED |

---

## 🎓 Why This Matters

### The SAGA Pattern Purpose

The SAGA pattern solves this problem:

> **"What if a later step fails after earlier steps have already committed changes?"**

### Example from Real Life

Imagine booking a vacation:

**Scenario A: Hotel booking fails immediately**
- ❌ Hotel: Booking failed
- 🔘 Flight: Never booked
- 🔘 Nothing to cancel

**Scenario B: Flight booking fails after hotel is confirmed**
- ✅ Hotel: Booking confirmed
- ❌ Flight: Booking failed
- 🔄 **Must cancel hotel!** (This is compensation)

### In Our System

**Payment Failure:**
- Payment gateway rejects the transaction
- Inventory was never reserved
- Nothing to compensate

**Inventory Failure:**
- Payment gateway charged the customer
- Inventory system says "out of stock"
- **Must refund the customer!** (compensation)

---

## 🔍 How to Verify Compensation

### Check 1: Order Status

```bash
# Get order details
curl http://localhost:8080/api/orders/{orderId} | jq

# Look for:
{
  "status": "CANCELLED",
  // Check previous status in logs to see if payment succeeded
}
```

### Check 2: Service Logs

```bash
# Watch the compensation flow
docker-compose logs -f order-service payment-service inventory-service

# Look for this sequence:
# 1. inventory-service: "Inventory reservation failed"
# 2. order-service: "Initiating payment refund"
# 3. order-service: "Refund command sent"
# 4. payment-service: "Refunding payment"
# 5. payment-service: "Payment refunded successfully"
# 6. order-service: "Order status updated to: CANCELLED"
```

### Check 3: Kafka Topics

```bash
# Check inventory-failed topic
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic inventory-failed \
  --from-beginning

# Check refund-payment topic (compensation command!)
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic refund-payment \
  --from-beginning

# Check payment-refunded topic (compensation result!)
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic payment-refunded \
  --from-beginning
```

---

## 🎯 Testing Strategy

### To Test Payment Failure (No Compensation)

```bash
# Create 10 orders - ~1 will fail at payment
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{
    "customerId": "TEST-'$i'",
    "customerEmail": "test'$i'@example.com",
    "totalAmount": 199.99,
    "items": [{"productId": "PROD-'$i'", "productName": "Test", "quantity": 1, "price": 199.99}]
  }'
  sleep 1
done

# Verify: Check logs for "Payment failed" but NO "Refund" messages
docker-compose logs payment-service | grep -i "failed"
docker-compose logs order-service | grep -i "refund" || echo "Good - no refunds for payment failures"
```

### To Test Inventory Failure (WITH Compensation)

```bash
# Create 20 orders - ~1 will fail at inventory
for i in {1..20}; do
  curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d '{
    "customerId": "TEST-'$i'",
    "customerEmail": "test'$i'@example.com",
    "totalAmount": 299.99,
    "items": [{"productId": "PROD-'$i'", "productName": "Test", "quantity": 1, "price": 299.99}]
  }'
  sleep 1
done

# Verify: Check logs for compensation sequence
docker-compose logs | grep -E "Inventory.*failed|Refund|refund" | head -20
```

**Or use the automated script:**

```bash
bash test-saga-compensation.sh
```

---

## ✅ Success Checklist

Your SAGA implementation is complete when:

- [ ] Payment failures result in CANCELLED orders with NO compensation
- [ ] Inventory failures result in CANCELLED orders WITH payment refund
- [ ] Logs show the complete compensation sequence
- [ ] Kafka topics contain refund-payment and payment-refunded messages
- [ ] Jaeger traces show the compensation span
- [ ] No orders stuck in intermediate states

---

## 📝 Documentation

- **SAGA_TESTING_GUIDE.md** - Detailed testing instructions
- **test-saga-compensation.sh** - Automated testing script
- **README.md** - System architecture
- **SHOW_AND_TELL.md** - Visual flow diagrams
