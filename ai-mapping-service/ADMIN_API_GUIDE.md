# Admin API Guide

## Authentication

All admin APIs require the `X-Admin-Key` header:

```bash
curl -H "X-Admin-Key: admin-secret-key" ...
```

Set the admin key via environment variable:
```bash
export ADMIN_API_KEY=your-secure-key
```

## API Reference

### 1. Save/Update Mapping Configuration

Create or update a mapping configuration for a partner.

```bash
POST /api/admin/mapping/{partnerId}
```

**Request:**
```bash
curl -X POST "http://localhost:8081/api/admin/mapping/partner-b" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0",
    "status": "ACTIVE",
    "createdBy": "admin@example.com",
    "mappings": [
      {
        "targetPath": "customer.id",
        "sourcePath": "$.account.accountNumber"
      },
      {
        "targetPath": "customer.name",
        "sourcePath": "$.account.holder",
        "transformationType": "uppercase"
      }
    ]
  }'
```

**Response:**
```json
{
  "partnerId": "partner-b",
  "version": "1.0",
  "status": "ACTIVE",
  "createdAt": "2024-03-10T10:00:00",
  "updatedAt": "2024-03-10T10:00:00",
  "createdBy": "admin@example.com",
  "mappings": [...]
}
```

### 2. Update Mapping Status

Change the status of a mapping configuration.

```bash
PUT /api/admin/mapping/{partnerId}/status
```

**Statuses:** `DRAFT`, `ACTIVE`, `INACTIVE`, `ARCHIVED`

**Request:**
```bash
curl -X PUT "http://localhost:8081/api/admin/mapping/partner-b/status" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ACTIVE"
  }'
```

### 3. Delete Mapping

Remove a mapping configuration from cache.

```bash
DELETE /api/admin/mapping/{partnerId}
```

**Request:**
```bash
curl -X DELETE "http://localhost:8081/api/admin/mapping/partner-b" \
  -H "X-Admin-Key: admin-secret-key"
```

### 4. List All Partners

Get all partner IDs with cached mappings.

```bash
GET /api/admin/mappings
```

**Request:**
```bash
curl "http://localhost:8081/api/admin/mappings" \
  -H "X-Admin-Key: admin-secret-key"
```

**Response:**
```json
["partner-a", "partner-b", "partner-c"]
```

### 5. Clear All Mapping Caches

Remove all mapping configurations from cache.

```bash
POST /api/admin/cache/clear/mappings
```

**Request:**
```bash
curl -X POST "http://localhost:8081/api/admin/cache/clear/mappings" \
  -H "X-Admin-Key: admin-secret-key"
```

### 6. Clear AI Response Cache

Remove all cached AI responses.

```bash
POST /api/admin/cache/clear/ai-responses
```

**Request:**
```bash
curl -X POST "http://localhost:8081/api/admin/cache/clear/ai-responses" \
  -H "X-Admin-Key: admin-secret-key"
```

### 7. Invalidate Specific Partner Cache

Remove cache for a specific partner.

```bash
POST /api/admin/cache/invalidate/{partnerId}
```

**Request:**
```bash
curl -X POST "http://localhost:8081/api/admin/cache/invalidate/partner-b" \
  -H "X-Admin-Key: admin-secret-key"
```

## Workflow Examples

### New Partner Onboarding

1. **Generate mapping using AI:**
```bash
curl -X POST "http://localhost:8081/api/ai-mapping/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "new-partner",
    "partnerSample": "{...}",
    "internalSample": "{...}",
    "createdBy": "admin@example.com"
  }'
```

2. **Review and activate:**
```bash
curl -X PUT "http://localhost:8081/api/admin/mapping/new-partner/status" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'
```

3. **Test transformation:**
```bash
curl -X POST "http://localhost:8080/api/transform/new-partner" \
  -H "Content-Type: application/json" \
  -d '{...partner data...}'
```

### Update Existing Mapping

1. **Get current mapping:**
```bash
curl "http://localhost:8081/api/ai-mapping/partner-a"
```

2. **Modify and save:**
```bash
curl -X POST "http://localhost:8081/api/admin/mapping/partner-a" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{...updated config...}'
```

3. **Invalidate cache:**
```bash
curl -X POST "http://localhost:8080/api/transform/invalidate/partner-a"
```

### Emergency Rollback

1. **Deactivate mapping:**
```bash
curl -X PUT "http://localhost:8081/api/admin/mapping/partner-a/status" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{"status": "INACTIVE"}'
```

2. **Clear cache:**
```bash
curl -X POST "http://localhost:8081/api/admin/cache/invalidate/partner-a" \
  -H "X-Admin-Key: admin-secret-key"
```

## Best Practices

### Security
- Rotate admin API keys regularly
- Use HTTPS in production
- Restrict admin API access by IP
- Audit all admin operations

### Cache Management
- Invalidate cache after updates
- Clear AI cache periodically to save memory
- Monitor cache hit rates
- Set appropriate TTLs

### Configuration Management
- Use DRAFT status for testing
- Test thoroughly before ACTIVE
- Keep version history
- Document changes in createdBy field

### Monitoring
- Log all admin operations
- Alert on failed updates
- Track cache invalidations
- Monitor transformation errors
