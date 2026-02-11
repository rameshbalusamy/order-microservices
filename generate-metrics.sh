#!/bin/bash

# Generate Metrics for Grafana/Prometheus Testing
# This script creates orders to populate metrics

echo "=========================================="
echo "METRICS GENERATION SCRIPT"
echo "=========================================="
echo ""
echo "This script will create orders to generate metrics"
echo "that you can view in Grafana and Prometheus"
echo ""

# Check if services are running
echo "Checking if Order Service is running..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "❌ Order Service is not responding"
    echo "Please start services with: docker-compose up"
    exit 1
fi

echo "✅ Order Service is running"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "PHASE 1: Generate Baseline Metrics"
echo "=========================================="
echo ""
echo "Creating 20 orders..."
echo ""

for i in {1..20}; do
    echo -n "Creating order $i... "
    
    response=$(curl -s -X POST http://localhost:8080/api/orders \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"METRICS-USER-$i\",
            \"customerEmail\": \"metrics$i@test.com\",
            \"totalAmount\": $((100 + RANDOM % 900)).99,
            \"items\": [{
                \"productId\": \"PROD-001\",
                \"productName\": \"Laptop\",
                \"quantity\": 1,
                \"price\": $((100 + RANDOM % 900)).99
            }]
        }")
    
    order_id=$(echo $response | jq -r '.orderId' 2>/dev/null)
    
    if [ "$order_id" != "null" ] && [ -n "$order_id" ]; then
        echo -e "${GREEN}✓${NC} $order_id"
    else
        echo -e "${YELLOW}⚠${NC} May have failed"
    fi
    
    sleep 0.5
done

echo ""
echo "✅ Baseline metrics generated"
echo ""

echo "=========================================="
echo "PHASE 2: Generate Load Pattern"
echo "=========================================="
echo ""
echo "Creating variable load for 30 seconds..."
echo ""

end_time=$(($(date +%s) + 30))
count=0

while [ $(date +%s) -lt $end_time ]; do
    ((count++))
    
    # Variable rate: 1-3 orders per iteration
    num_orders=$((1 + RANDOM % 3))
    
    for ((j=1; j<=num_orders; j++)); do
        curl -s -X POST http://localhost:8080/api/orders \
            -H "Content-Type: application/json" \
            -d "{
                \"customerId\": \"LOAD-TEST-$count-$j\",
                \"customerEmail\": \"load$count-$j@test.com\",
                \"totalAmount\": $((100 + RANDOM % 900)).99,
                \"items\": [{
                    \"productId\": \"PROD-001\",
                    \"productName\": \"Laptop\",
                    \"quantity\": 1,
                    \"price\": $((100 + RANDOM % 900)).99
                }]
            }" > /dev/null &
    done
    
    echo -n "."
    
    # Variable delay: 0.5-2 seconds
    sleep $(awk -v min=0.5 -v max=2 'BEGIN{srand(); print min+rand()*(max-min)}')
done

wait
echo ""
echo ""
echo "✅ Load pattern generated"
echo ""

echo "=========================================="
echo "PHASE 3: Generate Spike"
echo "=========================================="
echo ""
echo "Creating burst of 50 orders..."
echo ""

for i in {1..50}; do
    curl -s -X POST http://localhost:8080/api/orders \
        -H "Content-Type: application/json" \
        -d "{
            \"customerId\": \"SPIKE-TEST-$i\",
            \"customerEmail\": \"spike$i@test.com\",
            \"totalAmount\": $((100 + RANDOM % 900)).99,
            \"items\": [{
                \"productId\": \"PROD-001\",
                \"productName\": \"Laptop\",
                \"quantity\": 1,
                \"price\": $((100 + RANDOM % 900)).99
            }]
        }" > /dev/null &
    
    echo -n "."
    
    if [ $((i % 10)) -eq 0 ]; then
        sleep 0.1
    fi
done

wait
echo ""
echo ""
echo "✅ Spike generated"
echo ""

echo "=========================================="
echo "METRICS GENERATION COMPLETE!"
echo "=========================================="
echo ""
echo "📊 View your metrics now:"
echo ""
echo "1. Prometheus Queries:"
echo "   http://localhost:9090/graph"
echo ""
echo "   Try these queries:"
echo "   - Total requests: http_server_requests_seconds_count"
echo "   - Request rate: rate(http_server_requests_seconds_count[5m])"
echo "   - Memory usage: jvm_memory_used_bytes"
echo ""
echo "2. Grafana Dashboard:"
echo "   http://localhost:3000"
echo "   Login: admin/admin"
echo ""
echo "3. Import the pre-built dashboard:"
echo "   - Go to Dashboards → Import"
echo "   - Upload: grafana-dashboard.json"
echo "   - Or use Dashboard ID: 4701 (Spring Boot)"
echo ""
echo "📈 Expected Metrics:"
echo "   - ~100+ total orders created"
echo "   - Variable request rates over time"
echo "   - Spike in traffic visible"
echo "   - Response times varying"
echo "   - Memory and CPU usage patterns"
echo ""
echo "=========================================="
