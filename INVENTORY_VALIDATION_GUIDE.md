# Inventory Validation Testing Guide

## ✅ REAL Inventory Checking Now Implemented!

The system now **actually checks** if products exist and have sufficient stock before reserving inventory.

---

## 📦 Available Products in Inventory

When the Inventory Service starts, it automatically initializes these products:

| Product ID | Product Name | Initial Stock |
|-----------|--------------|---------------|
| `PROD-001` | Laptop | 100 units |
| `PROD-LAPTOP-001` | Dell XPS 15 Laptop | 50 units |
| `PROD-MONITOR-001` | 4K Monitor 27inch | 75 units |
| `PROD-KEYBOARD-001` | Mechanical Keyboard | 200 units |

---

## ✅ Positive Test Cases

### Test 1: Valid Product with Sufficient Stock

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "customerEmail": "test@example.com",
    "totalAmount": 299.99,
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "price": 299.99
    }]
  }'
```

**Expected Result**: ✅ Order completes successfully

**Logs:**
```
inventory-service | Checking inventory for product: PROD-001 (quantity: 1)
inventory-service | Reserving 1 units of product: PROD-001 (current available: 100)
inventory-service | Reserved 1 units of product: PROD-001
inventory-service | All inventory reserved successfully
```

---

### Test 2: Multiple Items Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-002",
    "customerEmail": "test2@example.com",
    "totalAmount": 1499.95,
    "items": [
      {
        "productId": "PROD-LAPTOP-001",
        "productName": "Dell XPS 15 Laptop",
        "quantity": 1,
        "price": 999.99
      },
      {
        "productId": "PROD-KEYBOARD-001",
        "productName": "Mechanical Keyboard",
        "quantity": 2,
        "price": 249.98
      }
    ]
  }'
```

**Expected Result**: ✅ Order completes successfully, all items reserved

---

## ❌ Negative Test Cases

### Test 3: Non-Existent Product

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-003",
    "customerEmail": "test3@example.com",
    "totalAmount": 599.99,
    "items": [{
      "productId": "PROD-NONEXISTENT",
      "productName": "Non-Existent Product",
      "quantity": 1,
      "price": 599.99
    }]
  }'
```

**Expected Result**: ❌ Order fails at inventory, payment is refunded (SAGA compensation!)

**Logs:**
```
payment-service   | Payment completed successfully
inventory-service | Product not found in inventory: PROD-NONEXISTENT (Product Name: Non-Existent Product)
inventory-service | Inventory reservation failed for order: ORD-XXXXXXXX
order-service     | Inventory reservation failed. Initiating payment refund
order-service     | Refund command sent for order: ORD-XXXXXXXX
payment-service   | Refunding payment for order: ORD-XXXXXXXX
payment-service   | Payment refunded successfully
order-service     | Order status updated to: CANCELLED
```

**Order Status:**
```json
{
  "orderId": "ORD-XXXXXXXX",
  "status": "CANCELLED",
  ...
}
```

**✅ SAGA Compensation Triggered!** Payment was refunded because inventory failed.

---

### Test 4: Insufficient Stock

First, let's exhaust the stock:

```bash
# Create orders to consume all PROD-001 stock (100 units)
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"BULK-$i\",
      \"customerEmail\": \"bulk$i@test.com\",
      \"totalAmount\": 299.99,
      \"items\": [{
        \"productId\": \"PROD-001\",
        \"productName\": \"Laptop\",
        \"quantity\": 1,
        \"price\": 299.99
      }]
    }" > /dev/null
  echo "Order $i created"
done
```

Now try to order when stock is exhausted:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-004",
    "customerEmail": "test4@example.com",
    "totalAmount": 299.99,
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "price": 299.99
    }]
  }'
```

**Expected Result**: ❌ Order fails due to insufficient stock, payment is refunded

**Logs:**
```
inventory-service | Insufficient stock for product: PROD-001. Available: 0, Requested: 1
inventory-service | Inventory reservation failed
order-service     | Initiating payment refund (SAGA compensation)
payment-service   | Payment refunded successfully
order-service     | Order CANCELLED
```

---

### Test 5: Requesting More Than Available

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-005",
    "customerEmail": "test5@example.com",
    "totalAmount": 4999.95,
    "items": [{
      "productId": "PROD-MONITOR-001",
      "productName": "4K Monitor",
      "quantity": 100,
      "price": 4999.95
    }]
  }'
```

**Expected**: ❌ Fails - only 75 monitors available, but 100 requested

**Logs:**
```
inventory-service | Insufficient stock for product: PROD-MONITOR-001. Available: 75, Requested: 100
```

---

## 🔍 Verification Commands

### Check Inventory Status

```bash
# View inventory service logs
docker-compose logs inventory-service | grep "available"

# Should show current stock levels
```

### Check Reservation Records

```bash
# Access inventory database
docker exec -it postgres-inventory psql -U inventoryuser -d inventorydb

# View inventory items
SELECT product_id, product_name, available_quantity, reserved_quantity 
FROM inventory_items;

# View reservations
SELECT order_id, product_id, quantity, status 
FROM inventory_reservations 
ORDER BY created_at DESC 
LIMIT 10;
```

---

## 📊 How It Works Now

### Previous Behavior (Wrong ❌)
```
Order Created → Payment Success → Inventory "Reserved" (fake) → Always Success
```
- No actual inventory checking
- Products could be non-existent
- Stock levels ignored

### Current Behavior (Correct ✅)
```
Order Created → Payment Success → Inventory Checks:
  1. Does product exist? ❌ → FAIL + Refund
  2. Sufficient stock? ❌ → FAIL + Refund
  3. All checks pass? ✅ → Reserve + Continue
```

---

## 🎯 Testing Scenarios

### Scenario A: Product Doesn't Exist (SAGA Compensation)

1. ✅ Order created
2. ✅ Payment succeeds
3. ❌ Inventory fails: "Product not found"
4. 🔄 **SAGA Compensation**: Payment refunded
5. ✅ Order cancelled

### Scenario B: Insufficient Stock (SAGA Compensation)

1. ✅ Order created
2. ✅ Payment succeeds
3. ❌ Inventory fails: "Insufficient stock"
4. 🔄 **SAGA Compensation**: Payment refunded
5. ✅ Order cancelled

### Scenario C: Valid Order (Success)

1. ✅ Order created
2. ✅ Payment succeeds
3. ✅ Inventory reserved (stock decremented)
4. ✅ Notification sent
5. ✅ Order completed

---

## 🧪 Complete Test Script

```bash
#!/bin/bash

echo "=== Test 1: Valid Product ==="
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "TEST-VALID",
    "customerEmail": "valid@test.com",
    "totalAmount": 299.99,
    "items": [{
      "productId": "PROD-001",
      "productName": "Laptop",
      "quantity": 1,
      "price": 299.99
    }]
  }'
echo ""
sleep 5

echo "=== Test 2: Non-Existent Product (Should trigger compensation) ==="
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "TEST-INVALID",
    "customerEmail": "invalid@test.com",
    "totalAmount": 599.99,
    "items": [{
      "productId": "PROD-DOES-NOT-EXIST",
      "productName": "Invalid Product",
      "quantity": 1,
      "price": 599.99
    }]
  }'
echo ""
sleep 5

echo "=== Test 3: Excessive Quantity (Should trigger compensation) ==="
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "TEST-EXCESSIVE",
    "customerEmail": "excessive@test.com",
    "totalAmount": 9999.99,
    "items": [{
      "productId": "PROD-LAPTOP-001",
      "productName": "Dell XPS 15",
      "quantity": 1000,
      "price": 9999.99
    }]
  }'
echo ""
sleep 5

echo "=== Checking logs for compensation events ==="
docker-compose logs inventory-service payment-service | grep -E "not found|Insufficient|Refund|refund" | tail -20
```

---

## ✅ Summary

**Now Implemented:**
- ✅ Products must exist in inventory table
- ✅ Stock availability is checked
- ✅ Stock is decremented on successful reservation
- ✅ Non-existent products trigger SAGA compensation
- ✅ Insufficient stock triggers SAGA compensation
- ✅ Multiple items are checked individually

**SAGA Compensation Works For:**
- Non-existent products
- Insufficient stock
- Any inventory validation failure
- Simulated failures (for testing)

**The system is now production-ready with real inventory management!**
