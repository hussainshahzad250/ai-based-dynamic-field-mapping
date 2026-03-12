# Architecture Overview

## System Components

### 1. AI Mapping Service (Port 8081)
Dedicated microservice for AI-powered mapping generation and management.

**Responsibilities:**
- Generate mapping configurations using AI
- Improve existing mappings
- Cache AI responses to reduce costs
- Store mapping configurations in Redis
- Provide admin APIs for configuration management

**Key Features:**
- Redis caching for AI responses (24h TTL)
- Redis caching for mapping configs (24h TTL)
- Support for multiple AI providers (OpenAI, Ollama, Anthropic)
- Admin API with key-based authentication

### 2. Mapping Engine (Port 8080)
High-performance transformation engine for runtime JSON mapping.

**Responsibilities:**
- Transform partner JSON to internal format
- Fetch mapping configs dynamically from cache or AI service
- Execute transformations using JSONPath
- Handle high-throughput transformation requests

**Key Features:**
- Dynamic config loading from Redis
- Fallback to AI service if cache miss
- Local cache for frequently used mappings
- Fast JSONPath-based transformations

### 3. Redis Cache
Centralized caching layer for both services.

**Cached Data:**
- Mapping configurations (key: `mapping:{partnerId}`)
- AI responses (key: `ai:response:{hash}`)
- TTL: 24 hours (configurable)

## Data Flow

### Mapping Generation Flow
```
Admin → AI Service → AI Provider (Ollama/OpenAI)
                  ↓
              Redis Cache
```

### Runtime Transformation Flow
```
Client → Mapping Engine → Redis Cache
                        ↓ (cache miss)
                   AI Service → Redis Cache
```

## API Endpoints

### AI Mapping Service (8081)

#### Public APIs
- `POST /api/ai-mapping/generate` - Generate new mapping
- `POST /api/ai-mapping/improve/{partnerId}` - Improve existing mapping
- `GET /api/ai-mapping/{partnerId}` - Get mapping config

#### Admin APIs (Requires X-Admin-Key header)
- `POST /api/admin/mapping/{partnerId}` - Save/update mapping
- `PUT /api/admin/mapping/{partnerId}/status` - Update status
- `DELETE /api/admin/mapping/{partnerId}` - Delete mapping
- `GET /api/admin/mappings` - List all partners
- `POST /api/admin/cache/clear/mappings` - Clear mapping cache
- `POST /api/admin/cache/clear/ai-responses` - Clear AI cache
- `POST /api/admin/cache/invalidate/{partnerId}` - Invalidate specific cache

### Mapping Engine (8080)

- `POST /api/transform/{partnerId}` - Transform JSON
- `POST /api/transform/invalidate/{partnerId}` - Invalidate cache

## Configuration

### AI Mapping Service
```yaml
ai:
  provider: ollama
  model: llama3.2
  endpoint: http://localhost:11434/api/generate
  cache:
    enabled: true
    ttl-hours: 24

admin:
  api-key: admin-secret-key
```

### Mapping Engine
```yaml
ai:
  service:
    url: http://localhost:8081
```

## Deployment

### Local Development
```bash
# Start Redis
docker run -d -p 6379:6379 redis:7-alpine

# Start AI Mapping Service
cd ai-mapping-service
mvn spring-boot:run

# Start Mapping Engine
cd mapping-engine
mvn spring-boot:run
```

### Docker Compose
```bash
docker-compose up -d
```

## Scaling Considerations

### Horizontal Scaling
- Both services are stateless
- Redis handles distributed caching
- Load balancer can distribute traffic

### Performance Optimization
- Redis caching reduces AI calls by 90%+
- Mapping configs cached for 24h
- AI responses cached to reduce costs
- Connection pooling for Redis

### High Availability
- Redis Sentinel for failover
- Multiple service instances behind load balancer
- Health checks for automatic recovery

## Security

### Authentication
- Admin APIs protected by API key
- Key passed via `X-Admin-Key` header
- Configurable via environment variable

### Data Protection
- Redis password authentication
- TLS for Redis connections (production)
- No sensitive data in logs

## Monitoring

### Key Metrics
- Cache hit rate (Redis)
- AI service response time
- Transformation throughput
- Error rates

### Health Checks
- `/actuator/health` (Spring Boot)
- Redis connectivity
- AI service availability

## Cost Optimization

### AI Usage
- Cache AI responses (24h TTL)
- Reuse mappings across requests
- Batch similar requests
- Use cheaper models for improvements

### Redis Usage
- Set appropriate TTLs
- Monitor memory usage
- Use Redis eviction policies
- Compress large configs
