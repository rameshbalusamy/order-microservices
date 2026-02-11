# Environment Configuration for Testing

## Controlling Failure Rates

You can adjust failure rates by setting environment variables in docker-compose.yml:

### Payment Service Failure Rate

```yaml
payment-service:
  environment:
    PAYMENT_FAILURE_RATE: "50"  # 50% failure rate for testing
```

### Inventory Service Failure Rate

```yaml
inventory-service:
  environment:
    INVENTORY_FAILURE_RATE: "30"  # 30% failure rate for testing
```

## Testing Configurations

### Configuration 1: Test Payment Failures (No Compensation)
```yaml
payment-service:
  environment:
    PAYMENT_FAILURE_RATE: "80"  # 80% will fail

inventory-service:
  environment:
    INVENTORY_FAILURE_RATE: "0"  # Never fail
```

### Configuration 2: Test Inventory Failures (WITH Compensation)
```yaml
payment-service:
  environment:
    PAYMENT_FAILURE_RATE: "0"  # Always succeed

inventory-service:
  environment:
    INVENTORY_FAILURE_RATE: "50"  # 50% will fail, triggering compensation!
```

### Configuration 3: Test Both Failures
```yaml
payment-service:
  environment:
    PAYMENT_FAILURE_RATE: "30"  # 30% fail

inventory-service:
  environment:
    INVENTORY_FAILURE_RATE: "40"  # 40% fail
```

## How to Apply

1. Edit `docker-compose.yml`
2. Add the environment variables under the service
3. Restart: `docker-compose down && docker-compose up --build`
