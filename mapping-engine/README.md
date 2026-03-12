# Mapping Engine

High-performance JSON transformation service with dynamic runtime mapping configuration.

## Features

- Dynamic mapping configuration from Redis cache
- Automatic fallback to AI Mapping Service
- Fast JSONPath-based transformations
- Support for nested objects and arrays
- Built-in transformations (uppercase, lowercase, date formatting, multiplication)
- Stateless design for horizontal scaling

## Quick Start

### Local Development

```bash
# Start Redis
docker run -d -p 6379:6379 redis:7-alpine

# Build and run
mvn clean install
mvn spring-boot:run
```

### Docker

```bash
# Build image
docker build -t mapping-engine .

# Run container
docker run -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e AI_SERVICE_URL=http://ai-mapping-service:8081 \
  mapping-engine
```

## Configuration

### application.yml

```yaml
server:
  port: 8080

spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

ai:
  service:
    url: ${AI_SERVICE_URL:http://localhost:8081}
```

### Environment Variables

- `REDIS_HOST`: Redis server hostname (default: localhost)
- `REDIS_PORT`: Redis server port (default: 6379)
- `AI_SERVICE_URL`: AI Mapping Service URL (default: http://localhost:8081)

## API Endpoints

### Transform Partner JSON

```bash
POST /api/transform/{partnerId}
Content-Type: application/json

{
  "client": {
    "clientId": "12345",
    "fullName": "john doe"
  },
  "payment": {
    "value": 99.99,
    "curr": "USD",
    "timestamp": "2024-03-09 14:30:00"
  }
}
```

**Response:**
```json
{
  "customer": {
    "id": "12345",
    "name": "JOHN DOE"
  },
  "transaction": {
    "amount": 9999,
    "currency": "USD",
    "date": "2024-03-09T14:30:00Z"
  }
}
```

### Invalidate Cache

```bash
POST /api/transform/invalidate/{partnerId}
```

## How It Works

1. **Request arrives** with partner JSON and partner ID
2. **Fetch mapping** from Redis cache
3. **If cache miss**, fetch from AI Mapping Service
4. **Apply transformations** using JSONPath and transformation rules
5. **Return** transformed JSON in internal format

## Supported Transformations

### uppercase / lowercase
```json
{
  "transformationType": "uppercase"
}
```

### dateFormat
```json
{
  "transformationType": "dateFormat",
  "transformationConfig": {
    "inputFormat": "yyyy-MM-dd HH:mm:ss",
    "outputFormat": "yyyy-MM-dd'T'HH:mm:ss'Z'"
  }
}
```

### multiply
```json
{
  "transformationType": "multiply",
  "transformationConfig": {
    "factor": 100
  }
}
```

### concat
```json
{
  "transformationType": "concat",
  "transformationConfig": {
    "prefix": "PREFIX_",
    "suffix": "_SUFFIX"
  }
}
```

## Performance

- **Cache hit**: < 10ms response time
- **Cache miss**: 50-100ms (includes AI service call)
- **Throughput**: 1000+ requests/second (with cache hits)
- **Horizontal scaling**: Stateless, add more instances as needed

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

- Cache hit rate
- Transformation latency
- Error rate
- Throughput

## Troubleshooting

### Connection to Redis Failed

```bash
# Check Redis is running
docker ps | grep redis

# Test Redis connection
redis-cli ping
```

### Mapping Not Found

```bash
# Check if mapping exists in AI service
curl http://localhost:8081/api/ai-mapping/{partnerId}

# Generate mapping if missing
curl -X POST http://localhost:8081/api/ai-mapping/generate \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### Transformation Errors

- Verify JSONPath expressions in mapping config
- Check input JSON structure matches expected format
- Review transformation configuration parameters
- Check logs for detailed error messages

## Development

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run Locally

```bash
mvn spring-boot:run
```

## Dependencies

- Spring Boot 3.2.0
- Spring Data Redis
- JSONPath 2.9.0
- Jackson (JSON processing)
- Lombok
- Lettuce (Redis client)
