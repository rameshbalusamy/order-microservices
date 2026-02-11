#!/bin/bash

# Complete File Verification Script
# Checks every single file in the project

echo "=========================================="
echo "COMPLETE FILE VERIFICATION"
echo "=========================================="
echo ""

MISSING=0
PRESENT=0

check_file() {
    if [ -f "$1" ]; then
        echo "✓ $1"
        ((PRESENT++))
        return 0
    else
        echo "✗ MISSING: $1"
        ((MISSING++))
        return 1
    fi
}

echo "📚 ROOT DOCUMENTATION"
echo "===================="
check_file "README.md"
check_file "SHOW_AND_TELL.md"
check_file "QUICKSTART.md"
check_file "PROJECT_SUMMARY.md"
check_file "ARCHITECTURE_DIAGRAMS.md"
check_file "INSTALLATION.md"
check_file "api-spec.yaml"
check_file "docker-compose.yml"
check_file "postman-collection.json"
check_file "setup-check.sh"
echo ""

echo "🔧 ORDER SERVICE"
echo "================"
check_file "order-service/pom.xml"
check_file "order-service/Dockerfile"
check_file "order-service/src/main/resources/application.yml"
check_file "order-service/src/main/java/com/orderms/order/OrderServiceApplication.java"
check_file "order-service/src/main/java/com/orderms/order/controller/OrderController.java"
check_file "order-service/src/main/java/com/orderms/order/controller/OrderStreamController.java"
check_file "order-service/src/main/java/com/orderms/order/service/OrderService.java"
check_file "order-service/src/main/java/com/orderms/order/repository/OrderRepository.java"
check_file "order-service/src/main/java/com/orderms/order/model/Order.java"
check_file "order-service/src/main/java/com/orderms/order/model/OrderItem.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/OrderEventConsumer.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/OrderCreatedEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/PaymentCompletedEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/PaymentFailedEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/InventoryReservedEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/InventoryFailedEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/NotificationSentEvent.java"
check_file "order-service/src/main/java/com/orderms/order/kafka/RefundPaymentCommand.java"
echo ""

echo "💳 PAYMENT SERVICE"
echo "=================="
check_file "payment-service/pom.xml"
check_file "payment-service/Dockerfile"
check_file "payment-service/src/main/resources/application.yml"
check_file "payment-service/src/main/java/com/orderms/payment/PaymentServiceApplication.java"
check_file "payment-service/src/main/java/com/orderms/payment/service/PaymentService.java"
check_file "payment-service/src/main/java/com/orderms/payment/repository/PaymentRepository.java"
check_file "payment-service/src/main/java/com/orderms/payment/model/Payment.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/PaymentEventConsumer.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/OrderCreatedEvent.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/PaymentCompletedEvent.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/PaymentFailedEvent.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/RefundPaymentCommand.java"
check_file "payment-service/src/main/java/com/orderms/payment/kafka/PaymentRefundedEvent.java"
echo ""

echo "📦 INVENTORY SERVICE"
echo "===================="
check_file "inventory-service/pom.xml"
check_file "inventory-service/Dockerfile"
check_file "inventory-service/src/main/resources/application.yml"
check_file "inventory-service/src/main/java/com/orderms/inventory/InventoryServiceApplication.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/service/InventoryService.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/repository/InventoryItemRepository.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/repository/InventoryReservationRepository.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/model/InventoryItem.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/model/InventoryReservation.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/kafka/InventoryEventConsumer.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/kafka/PaymentCompletedEvent.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/kafka/InventoryReservedEvent.java"
check_file "inventory-service/src/main/java/com/orderms/inventory/kafka/InventoryFailedEvent.java"
echo ""

echo "📧 NOTIFICATION SERVICE"
echo "======================="
check_file "notification-service/pom.xml"
check_file "notification-service/Dockerfile"
check_file "notification-service/src/main/resources/application.yml"
check_file "notification-service/src/main/java/com/orderms/notification/NotificationServiceApplication.java"
check_file "notification-service/src/main/java/com/orderms/notification/service/NotificationService.java"
check_file "notification-service/src/main/java/com/orderms/notification/kafka/NotificationEventConsumer.java"
check_file "notification-service/src/main/java/com/orderms/notification/kafka/InventoryReservedEvent.java"
check_file "notification-service/src/main/java/com/orderms/notification/kafka/NotificationSentEvent.java"
echo ""

echo "📊 MONITORING"
echo "============="
check_file "monitoring/prometheus.yml"
check_file "monitoring/grafana/datasources/prometheus.yml"
check_file "monitoring/grafana/dashboards/dashboard-provider.yml"
echo ""

echo "=========================================="
echo "SUMMARY"
echo "=========================================="
echo "✓ Files Present: $PRESENT"
echo "✗ Files Missing: $MISSING"
echo ""

if [ $MISSING -eq 0 ]; then
    echo "🎉 ALL FILES VERIFIED - PROJECT IS COMPLETE!"
    echo ""
    echo "You can now run:"
    echo "  docker-compose up --build"
    exit 0
else
    echo "⚠️  SOME FILES ARE MISSING - Please check above"
    exit 1
fi
