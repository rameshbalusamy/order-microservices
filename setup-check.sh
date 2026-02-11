#!/bin/bash

# Order Microservices SAGA - Complete Setup and Verification Script
# This script verifies all files are present and provides setup instructions

set -e

echo "=========================================="
echo "Order Microservices SAGA - Setup Checker"
echo "=========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a file exists
check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        return 0
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        return 1
    fi
}

# Function to check if a directory exists
check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1/"
        return 0
    else
        echo -e "${RED}✗${NC} $1/ (MISSING)"
        return 1
    fi
}

echo "Checking project structure..."
echo ""

# Check main documentation
echo "📚 Documentation:"
check_file "README.md"
check_file "SHOW_AND_TELL.md"
check_file "QUICKSTART.md"
check_file "PROJECT_SUMMARY.md"
check_file "ARCHITECTURE_DIAGRAMS.md"
check_file "api-spec.yaml"
check_file "docker-compose.yml"
check_file "postman-collection.json"
echo ""

# Check Order Service
echo "📦 Order Service:"
check_dir "order-service"
check_file "order-service/pom.xml"
check_file "order-service/Dockerfile"
check_file "order-service/src/main/resources/application.yml"
check_file "order-service/src/main/java/com/orderms/order/OrderServiceApplication.java"
check_file "order-service/src/main/java/com/orderms/order/model/Order.java"
check_file "order-service/src/main/java/com/orderms/order/model/OrderItem.java"
check_file "order-service/src/main/java/com/orderms/order/service/OrderService.java"
check_file "order-service/src/main/java/com/orderms/order/controller/OrderController.java"
check_file "order-service/src/main/java/com/orderms/order/controller/OrderStreamController.java"
echo ""

# Check Payment Service
echo "💳 Payment Service:"
check_dir "payment-service"
check_file "payment-service/pom.xml"
check_file "payment-service/Dockerfile"
check_file "payment-service/src/main/resources/application.yml"
check_file "payment-service/src/main/java/com/orderms/payment/PaymentServiceApplication.java"
check_file "payment-service/src/main/java/com/orderms/payment/model/Payment.java"
check_file "payment-service/src/main/java/com/orderms/payment/service/PaymentService.java"
echo ""

# Check Inventory Service
echo "📦 Inventory Service:"
check_dir "inventory-service"
check_file "inventory-service/pom.xml"
check_file "inventory-service/Dockerfile"
check_file "inventory-service/src/main/resources/application.yml"
check_file "inventory-service/src/main/java/com/orderms/inventory/InventoryServiceApplication.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/model/InventoryItem.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/model/InventoryReservation.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/service/InventoryService.java"
echo ""

# Check Notification Service
echo "📧 Notification Service:"
check_dir "notification-service"
check_file "notification-service/pom.xml"
check_file "notification-service/Dockerfile"
check_file "notification-service/src/main/resources/application.yml"
check_file "notification-service/src/main/java/com/orderms/notification/NotificationServiceApplication.java"
check_file "notification-service/src/main/java/com/orderms/notification/service/NotificationService.java"
echo ""

# Check Monitoring
echo "📊 Monitoring Configuration:"
check_dir "monitoring"
check_file "monitoring/prometheus.yml"
check_dir "monitoring/grafana"
check_file "monitoring/grafana/datasources/prometheus.yml"
echo ""

# Count Java files
JAVA_COUNT=$(find . -name "*.java" -type f | wc -l)
echo -e "${YELLOW}Total Java files:${NC} $JAVA_COUNT"
echo ""

echo "=========================================="
echo "🚀 QUICK START INSTRUCTIONS"
echo "=========================================="
echo ""
echo "1. Prerequisites:"
echo "   - Docker & Docker Compose installed"
echo "   - 8GB RAM minimum"
echo "   - Ports available: 8080-8083, 9090, 3000, 16686, 5432-5434, 9092"
echo ""
echo "2. Start the system:"
echo "   docker-compose up --build"
echo ""
echo "3. Wait 2-3 minutes for all services to start"
echo ""
echo "4. Test it:"
echo "   curl -X POST http://localhost:8080/api/orders \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{
echo "       \"customerId\": \"CUST-001\",
echo "       \"customerEmail\": \"test@example.com\",
echo "       \"totalAmount\": 299.99,
echo "       \"items\": [{
echo "         \"productId\": \"PROD-001\",
echo "         \"productName\": \"Laptop\",
echo "         \"quantity\": 1,
echo "         \"price\": 299.99
echo "       }]
echo "     }'"
echo ""
echo "5. Access monitoring:"
echo "   - Grafana:     http://localhost:3000 (admin/admin)"
echo "   - Jaeger:      http://localhost:16686"
echo "   - Prometheus:  http://localhost:9090"
echo ""
echo "=========================================="
echo "📖 DOCUMENTATION"
echo "=========================================="
echo ""
echo "- QUICKSTART.md      - 5-minute setup guide"
echo "- README.md          - Complete documentation"
echo "- SHOW_AND_TELL.md   - Detailed architecture & flows"
echo "- api-spec.yaml      - OpenAPI specification"
echo ""
echo "=========================================="
echo "✅ Setup verification complete!"
echo "=========================================="
