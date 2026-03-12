# Quick Start Guide

## Prerequisites

- Docker & Docker Compose
- Ollama (for local AI)
- Java 17+ (for local development)

## Option 1: Docker Compose (Recommended)

```bash
# Start all services
docker-compose up -d

# Check logs
docker-compose logs -f

# Stop services
docker-compose down
```

Services will be available at:
- AI Mapping Service: http://localhost:8081
- Mapping Engine: http://localhost:8080
- Redis: localhost:6379

## Option 2: Local Development

### 1. Start Redis
```bash
docker run -d -p 6379:6379 redis:7-alpine
```

### 2. Start Ollama
```bash
ollama serve
ollama pull llama3.2
```

### 3. Start AI Mapping Service
```bash
cd ai-mapping-service
mvn spring-boot:run
```

### 4. Start Mapping Engine
```bash
cd mapping-engine
mvn spring-boot:run
```

## Test the Setup

### 1. Generate Mapping
```bash
curl -X POST "http://localhost:8081/api/ai-mapping/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "test-partner",
    "partnerSample": "{\"user\": {\"id\": \"123\", \"name\": \"John\"}}",
    "internalSample": "{\"customer\": {\"customerId\": \"123\", \"fullName\": \"JOHN\"}}",
    "createdBy": "admin"
  }'
```

### 2. Activate Mapping
```bash
curl -X PUT "http://localhost:8081/api/admin/mapping/test-partner/status" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'
```

### 3. Transform Data
```bash
curl -X POST "http://localhost:8080/api/transform/test-partner" \
  -H "Content-Type: application/json" \
  -d '{"user": {"id": "123", "name": "John"}}'
```

## Environment Variables

Create `.env` file:
```bash
# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# AI Configuration
AI_PROVIDER=ollama
AI_MODEL=llama3.2
AI_ENDPOINT=http://localhost:11434/api/generate

# Admin
ADMIN_API_KEY=your-secure-key
```

## Next Steps

- Read [ADMIN_API_GUIDE.md](ADMIN_API_GUIDE.md) for admin operations
- Check [ARCHITECTURE.md](ARCHITECTURE.md) for system design
- See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues
