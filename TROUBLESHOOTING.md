# Troubleshooting Guide

## Common Issues and Solutions

### 🔴 Database Connection Errors

#### Issue: `FATAL: database "orderuser" does not exist`

**Cause**: PostgreSQL is looking for a database with the same name as the user, or old volume data exists.

**Solution**:
```bash
# Stop all services
docker-compose down

# Remove all volumes (this will delete all data)
docker-compose down -v

# Start fresh
docker-compose up --build
```

**Why this works**: The `-v` flag removes old PostgreSQL data volumes, allowing the containers to initialize fresh databases with the correct names.

---

### 🔴 Port Already in Use

#### Issue: `Error starting userland proxy: listen tcp4 0.0.0.0:8080: bind: address already in use`

**Solution**:

**Linux/Mac**:
```bash
# Find the process using the port
lsof -i :8080

# Kill it
kill -9 <PID>
```

**Windows**:
```cmd
# Find the process
netstat -ano | findstr :8080

# Kill it
taskkill /PID <PID> /F
```

**Alternative**: Change the port in `docker-compose.yml`:
```yaml
ports:
  - "8090:8080"  # Change 8080 to 8090
```

---

### 🔴 Services Won't Start

#### Issue: Services keep restarting or failing

**Solution 1: Check Logs**
```bash
# View logs for specific service
docker-compose logs -f order-service
docker-compose logs -f payment-service

# View all logs
docker-compose logs -f
```

**Solution 2: Restart Individual Service**
```bash
docker-compose restart order-service
```

**Solution 3: Rebuild**
```bash
docker-compose up --build order-service
```

---

### 🔴 Kafka Connection Issues

#### Issue: `org.apache.kafka.common.errors.TimeoutException`

**Cause**: Kafka hasn't fully started or services started before Kafka was ready.

**Solution**:
```bash
# Restart Kafka and Zookeeper
docker-compose restart kafka zookeeper

# Wait 30 seconds
sleep 30

# Restart microservices
docker-compose restart order-service payment-service inventory-service notification-service
```

---

### 🔴 Out of Memory

#### Issue: `Docker daemon out of memory` or containers being killed

**Solution**:

**Docker Desktop**:
1. Open Docker Desktop
2. Go to Settings → Resources
3. Increase Memory to at least **8GB**
4. Click "Apply & Restart"

**Linux**:
```bash
# Check available memory
free -h

# If low, close other applications or increase system RAM
```

---

### 🔴 Build Failures

#### Issue: Maven build fails during `docker-compose up --build`

**Solution 1: Clear Maven Cache**
```bash
docker-compose down
docker system prune -a
docker-compose up --build
```

**Solution 2: Build Services Individually**
```bash
cd order-service
mvn clean package
cd ..

# Repeat for other services
```

---

### 🔴 Health Checks Failing

#### Issue: Services show as "unhealthy" in `docker-compose ps`

**Solution**:
```bash
# Check health status
docker-compose ps

# Inspect specific service
docker inspect postgres-order

# Wait longer - services can take 2-3 minutes to become healthy
# Health checks have retries and start_period configured
```

---

### 🔴 Cannot Create Order

#### Issue: `POST /api/orders` returns 500 error

**Checklist**:
1. **Are all services running?**
   ```bash
   docker-compose ps
   # All should show "Up" status
   ```

2. **Check service health**:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8081/actuator/health
   curl http://localhost:8082/actuator/health
   curl http://localhost:8083/actuator/health
   ```

3. **Check Kafka**:
   ```bash
   docker-compose logs kafka | tail -50
   ```

4. **Check database connections**:
   ```bash
   docker-compose logs postgres-order | tail -20
   ```

---

### 🔴 SSE Stream Not Working

#### Issue: `/api/orders/{id}/stream` returns 404 or doesn't stream

**Solution**:

Test with curl:
```bash
curl -N http://localhost:8080/api/orders/{orderId}/stream
```

**Note**: 
- Postman may not show SSE properly - use curl or browser
- Make sure order ID exists first

---

### 🔴 Grafana/Jaeger Not Accessible

#### Issue: Cannot access monitoring tools

**Solution**:
```bash
# Check if containers are running
docker-compose ps grafana prometheus jaeger

# Restart monitoring stack
docker-compose restart grafana prometheus jaeger

# Access URLs:
# Grafana: http://localhost:3000 (admin/admin)
# Jaeger: http://localhost:16686
# Prometheus: http://localhost:9090
```

---

### 🔴 Services Start But Don't Work Together

#### Issue: Services run but orders don't process

**Solution**: Check Kafka topics:
```bash
# Enter Kafka container
docker exec -it kafka bash

# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Should see: order-created, payment-completed, etc.

# Check consumer groups
kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

---

## Complete Reset

If nothing works, perform a complete reset:

```bash
# Stop everything
docker-compose down -v

# Remove all Docker resources (WARNING: affects all containers)
docker system prune -a --volumes

# Rebuild from scratch
docker-compose up --build
```

---

## Getting Help

1. **Run verification script**:
   ```bash
   bash verify-files.sh
   ```

2. **Check logs for errors**:
   ```bash
   docker-compose logs | grep -i error
   docker-compose logs | grep -i exception
   ```

3. **Verify network**:
   ```bash
   docker network ls
   docker network inspect order-microservices-saga_microservices-network
   ```

4. **Check Docker resources**:
   ```bash
   docker stats
   ```

---

## Prevention Tips

✅ **Before Starting**:
- Ensure 8GB+ RAM available
- Close unnecessary applications
- Check all required ports are free
- Have stable internet for image downloads

✅ **Clean Start**:
```bash
# Always start clean after errors
docker-compose down -v
docker-compose up --build
```

✅ **Monitor Resources**:
```bash
# Watch logs in one terminal
docker-compose logs -f

# Check status in another
watch docker-compose ps
```

---

## Quick Reference Commands

```bash
# Start everything
docker-compose up --build

# Start in background
docker-compose up --build -d

# Stop everything
docker-compose down

# Stop and remove volumes
docker-compose down -v

# View logs
docker-compose logs -f <service-name>

# Restart service
docker-compose restart <service-name>

# Rebuild service
docker-compose up --build <service-name>

# Check status
docker-compose ps

# Execute command in container
docker exec -it <container-name> bash
```

---

**Still stuck?** Check the main README.md for architecture details or INSTALLATION.md for setup steps.
