#!/bin/bash

# SAGA Compensation Testing Script
# This script helps test negative scenarios and SAGA compensation

set -e

echo "=========================================="
echo "SAGA COMPENSATION TESTING SCRIPT"
echo "=========================================="
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Base URL
BASE_URL="http://localhost:8080"

# Function to create order
create_order() {
    local customer_id=$1
    local email=$2
    local amount=$3
    
    curl -s -X POST "${BASE_URL}/api/orders" \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"${customer_id}\",
            \"customerEmail\": \"${email}\",
            \"totalAmount\": ${amount},
            \"items\": [{
                \"productId\": \"PROD-TEST-001\",
                \"productName\": \"Test Product\",
                \"quantity\": 1,
                \"price\": ${amount}
            }]
        }"
}

# Function to check order status
check_order_status() {
    local order_id=$1
    curl -s "${BASE_URL}/api/orders/${order_id}"
}

echo "=========================================="
echo "TEST 1: Payment Failure (No Compensation)"
echo "=========================================="
echo ""
echo "Creating 10 orders to trigger payment failure..."
echo "Expected: ~1 order will fail at payment (10% failure rate)"
echo ""

payment_failed_orders=0
for i in {1..10}; do
    echo -n "Creating order $i..."
    
    result=$(create_order "PAYMENT-TEST-$i" "payment$i@test.com" "199.99")
    order_id=$(echo $result | jq -r '.orderId')
    
    if [ "$order_id" != "null" ]; then
        echo " Created: $order_id"
        
        # Wait for processing
        sleep 4
        
        # Check status
        status=$(check_order_status "$order_id" | jq -r '.status')
        
        if [ "$status" == "CANCELLED" ] || [ "$status" == "PAYMENT_FAILED" ]; then
            echo -e "${RED}❌ Order $order_id - Payment FAILED${NC}"
            echo "   Status: $status"
            echo "   ⚠️  No compensation needed (inventory never reserved)"
            ((payment_failed_orders++))
        else
            echo -e "${GREEN}✅ Order $order_id - Status: $status${NC}"
        fi
    fi
    echo ""
done

echo ""
echo "=========================================="
echo "TEST 1 RESULTS"
echo "=========================================="
echo -e "Payment failures: ${RED}$payment_failed_orders${NC} out of 10 orders"
echo -e "${YELLOW}Note: Payment failure does NOT trigger compensation${NC}"
echo ""

echo "=========================================="
echo "TEST 2: Inventory Failure (WITH Compensation)"
echo "=========================================="
echo ""
echo "Creating 20 orders to trigger inventory failure..."
echo "Expected: ~1 order will fail at inventory (5% failure rate)"
echo "Expected: Payment REFUND should be triggered!"
echo ""

inventory_failed_orders=0
compensation_triggered=0

for i in {1..20}; do
    echo -n "Creating order $i..."
    
    result=$(create_order "INVENTORY-TEST-$i" "inventory$i@test.com" "299.99")
    order_id=$(echo $result | jq -r '.orderId')
    
    if [ "$order_id" != "null" ]; then
        echo " Created: $order_id"
        
        # Wait longer for full SAGA flow
        sleep 5
        
        # Check final status
        order_data=$(check_order_status "$order_id")
        status=$(echo $order_data | jq -r '.status')
        
        if [ "$status" == "CANCELLED" ]; then
            # Check if it was inventory failure by looking at logs
            echo -e "${RED}❌ Order $order_id - CANCELLED${NC}"
            
            # Check logs to determine if it was inventory failure
            if docker-compose logs inventory-service 2>/dev/null | grep -q "$order_id.*failed"; then
                echo -e "   ${BLUE}📦 Inventory failure detected${NC}"
                echo -e "   ${YELLOW}🔄 Checking for compensation...${NC}"
                
                # Check for refund in logs
                if docker-compose logs payment-service 2>/dev/null | grep -q "$order_id.*[Rr]efund"; then
                    echo -e "   ${GREEN}✅ COMPENSATION SUCCESSFUL - Payment refunded!${NC}"
                    ((compensation_triggered++))
                else
                    echo -e "   ${RED}⚠️  Compensation may not have triggered${NC}"
                fi
                
                ((inventory_failed_orders++))
            elif docker-compose logs payment-service 2>/dev/null | grep -q "$order_id.*failed"; then
                echo "   💳 Payment failure (no compensation needed)"
            fi
        else
            echo -e "${GREEN}✅ Order $order_id - Status: $status${NC}"
        fi
    fi
    echo ""
done

echo ""
echo "=========================================="
echo "TEST 2 RESULTS"
echo "=========================================="
echo -e "Inventory failures: ${RED}$inventory_failed_orders${NC} out of 20 orders"
echo -e "Compensations triggered: ${GREEN}$compensation_triggered${NC}"
echo ""

if [ $compensation_triggered -gt 0 ]; then
    echo -e "${GREEN}🎉 SAGA COMPENSATION IS WORKING!${NC}"
    echo ""
    echo "To see the compensation flow in logs:"
    echo "  docker-compose logs order-service payment-service inventory-service | grep -E 'Refund|refund|CANCELLED'"
else
    echo -e "${YELLOW}⚠️  No inventory failures detected in this run${NC}"
    echo "Try running the script again or increase the number of orders"
fi

echo ""
echo "=========================================="
echo "TEST 3: Detailed Compensation Verification"
echo "=========================================="
echo ""
echo "Analyzing logs for compensation evidence..."
echo ""

# Search logs for refund events
echo "Searching for refund commands in Order Service..."
refund_commands=$(docker-compose logs order-service 2>/dev/null | grep -c "Refund command" || echo "0")
echo -e "Found: ${BLUE}$refund_commands${NC} refund commands"

echo ""
echo "Searching for payment refunds in Payment Service..."
payment_refunds=$(docker-compose logs payment-service 2>/dev/null | grep -c "refunded" || echo "0")
echo -e "Found: ${BLUE}$payment_refunds${NC} payment refunds"

echo ""
echo "Searching for inventory failures..."
inventory_failures=$(docker-compose logs inventory-service 2>/dev/null | grep -c "failed" || echo "0")
echo -e "Found: ${BLUE}$inventory_failures${NC} inventory failures"

echo ""
echo "=========================================="
echo "OVERALL SUMMARY"
echo "=========================================="
echo ""
echo "✅ Payment Failures (No Compensation): $payment_failed_orders"
echo "✅ Inventory Failures (WITH Compensation): $inventory_failed_orders"
echo "✅ Successful Compensations: $compensation_triggered"
echo ""

if [ $compensation_triggered -gt 0 ]; then
    echo -e "${GREEN}SUCCESS: SAGA compensation pattern is working correctly!${NC}"
    echo ""
    echo "The system correctly:"
    echo "  1. Detected inventory failure after payment success"
    echo "  2. Triggered compensation (payment refund)"
    echo "  3. Cancelled the order"
else
    echo -e "${YELLOW}INFO: Run script multiple times to trigger inventory failures${NC}"
    echo "Or check logs manually:"
    echo "  docker-compose logs -f | grep -E 'failed|Refund|refund'"
fi

echo ""
echo "=========================================="
echo "View Detailed Logs"
echo "=========================================="
echo ""
echo "To watch SAGA compensation in real-time:"
echo "  docker-compose logs -f order-service payment-service inventory-service"
echo ""
echo "To see only compensation events:"
echo "  docker-compose logs | grep -E 'Refund|refund|CANCELLED' | grep -v 'Payment failed'"
echo ""
