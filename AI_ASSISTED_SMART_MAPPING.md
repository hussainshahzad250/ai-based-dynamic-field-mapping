## AI-Assisted Smart Mapping for Large JSONs

### Overview

The AI-Assisted Smart Mapping Controller combines the speed of intelligent field matching with the accuracy of AI, specifically designed for handling large JSONs with 100+ fields.

### Key Features

1. **Hybrid Approach** - Smart matching first, AI for unmapped fields
2. **Quality Analysis** - Analyze mapping coverage and get suggestions
3. **Enhancement** - Improve existing mappings with AI
4. **Statistics** - Detailed mapping statistics and reports
5. **Batch Processing** - Generate mappings for multiple partners
6. **Comparison** - Compare Smart vs AI approaches

### API Endpoints

#### 1. Generate Hybrid Mapping

```bash
POST /api/ai-smart-mapping/generate
```

**Request:**
```json
{
  "partnerId": "large-partner",
  "partnerSample": "{...100+ fields...}",
  "internalSample": "{...100+ fields...}",
  "createdBy": "admin@example.com",
  "useAI": true
}
```

**Response:**
```json
{
  "partnerId": "large-partner",
  "version": "1.0",
  "status": "DRAFT",
  "createdAt": "2024-03-10T10:00:00",
  "mappings": [
    {
      "targetPath": "customer.id",
      "sourcePath": "$.user.userId"
    }
    // ... 100+ mappings
  ]
}
```

**Parameters:**
- `useAI`: `false` = Smart matching only (fast, free)
- `useAI`: `true` = Hybrid approach (smart + AI for unmapped fields)

#### 2. Analyze Mapping Quality

```bash
POST /api/ai-smart-mapping/analyze/{partnerId}
```

**Request:**
```json
{
  "partnerSample": "{...partner JSON...}",
  "useAI": true
}
```

**Response:**
```json
{
  "partnerId": "large-partner",
  "totalFields": 120,
  "mappedFields": 105,
  "unmappedFields": 15,
  "coveragePercentage": 87.5,
  "unmappedFieldsList": [
    "$.user.metadata.customField1",
    "$.user.metadata.customField2"
  ],
  "suggestions": [
    "Coverage is good but consider mapping remaining fields",
    "Review transformation accuracy for date fields"
  ],
  "potentialIssues": {
    "transaction.date": "Transformation type set but config missing"
  }
}
```

#### 3. Enhance Existing Mapping

```bash
POST /api/ai-smart-mapping/enhance/{partnerId}
```

**Request:**
```json
{
  "partnerSample": "{...partner JSON...}",
  "internalSample": "{...internal JSON...}"
}
```

Uses AI to improve existing mappings and add missing fields.

#### 4. Get Mapping Statistics

```bash
GET /api/ai-smart-mapping/stats/{partnerId}
```

**Response:**
```json
{
  "partnerId": "large-partner",
  "totalMappings": 105,
  "withTransformations": 25,
  "withoutTransformations": 80,
  "transformationTypes": {
    "uppercase": 10,
    "dateFormat": 8,
    "multiply": 5,
    "lowercase": 2
  },
  "averageFieldDepth": 2.3,
  "generationMethod": "hybrid"
}
```

#### 5. Batch Generate Mappings

```bash
POST /api/ai-smart-mapping/batch-generate
```

**Request:**
```json
{
  "partners": [
    {
      "partnerId": "partner-1",
      "partnerSample": "{...}",
      "internalSample": "{...}",
      "createdBy": "admin"
    },
    {
      "partnerId": "partner-2",
      "partnerSample": "{...}",
      "internalSample": "{...}",
      "createdBy": "admin"
    }
  ]
}
```

**Response:**
```json
{
  "totalPartners": 2,
  "successCount": 2,
  "failureCount": 0,
  "successfulPartners": ["partner-1", "partner-2"],
  "failures": {},
  "totalTimeMs": 5432
}
```

#### 6. Compare Approaches

```bash
POST /api/ai-smart-mapping/compare
```

**Request:**
```json
{
  "partnerId": "test-partner",
  "partnerSample": "{...}",
  "internalSample": "{...}"
}
```

**Response:**
```json
{
  "partnerId": "test-partner",
  "smartMapping": {
    "mappingsGenerated": 95,
    "timeMs": 2340,
    "coveragePercentage": 85.5,
    "cost": "Free"
  },
  "aiMapping": {
    "mappingsGenerated": 108,
    "timeMs": 45230,
    "coveragePercentage": 97.2,
    "cost": "~$0.01-0.05 per request"
  },
  "recommendation": "AI mapping provides significantly better coverage. Consider hybrid approach."
}
```

### Complete Workflow

#### Scenario 1: Large JSON (100+ fields) - Hybrid Approach

```bash
# Step 1: Generate hybrid mapping (Smart + AI)
curl -X POST "http://localhost:8081/api/ai-smart-mapping/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "large-partner",
    "partnerSample": "'"$(cat partner-large.json | jq -c .)"'",
    "internalSample": "'"$(cat internal-large.json | jq -c .)"'",
    "createdBy": "admin@example.com",
    "useAI": true
  }'

# Step 2: Analyze quality
curl -X POST "http://localhost:8081/api/ai-smart-mapping/analyze/large-partner" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerSample": "'"$(cat partner-large.json | jq -c .)"'",
    "useAI": true
  }'

# Step 3: Get statistics
curl "http://localhost:8081/api/ai-smart-mapping/stats/large-partner"

# Step 4: Activate mapping
curl -X PUT "http://localhost:8081/api/admin/mapping/large-partner/status" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{"status": "ACTIVE"}'

# Step 5: Transform data
curl -X POST "http://localhost:8080/api/transform/large-partner" \
  -H "Content-Type: application/json" \
  -d @partner-large.json
```

#### Scenario 2: Fast Generation (Smart Only)

```bash
# Generate without AI (faster, free)
curl -X POST "http://localhost:8081/api/ai-smart-mapping/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "fast-partner",
    "partnerSample": "...",
    "internalSample": "...",
    "useAI": false
  }'
```

#### Scenario 3: Enhance Existing Mapping

```bash
# Already have smart mapping, enhance with AI
curl -X POST "http://localhost:8081/api/ai-smart-mapping/enhance/existing-partner" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerSample": "...",
    "internalSample": "..."
  }'
```

#### Scenario 4: Batch Processing

```bash
# Generate mappings for multiple partners
curl -X POST "http://localhost:8081/api/ai-smart-mapping/batch-generate" \
  -H "Content-Type: application/json" \
  -d '{
    "partners": [
      {
        "partnerId": "partner-1",
        "partnerSample": "...",
        "internalSample": "...",
        "createdBy": "admin"
      },
      {
        "partnerId": "partner-2",
        "partnerSample": "...",
        "internalSample": "...",
        "createdBy": "admin"
      }
    ]
  }'
```

### Decision Matrix

| Scenario | Recommended Approach | Reason |
|----------|---------------------|---------|
| 100+ fields, high accuracy needed | Hybrid (useAI: true) | Best coverage |
| 100+ fields, speed priority | Smart only (useAI: false) | Fast, free |
| < 50 fields | AI only | Better accuracy for small JSONs |
| Multiple partners | Batch with Smart | Efficient bulk processing |
| Existing mapping needs improvement | Enhance | Incremental improvement |

### Performance Comparison

| Approach | 100 Fields | 500 Fields | 1000 Fields | Cost |
|----------|-----------|-----------|-------------|------|
| Smart Only | 2-3s | 10-15s | 30-40s | Free |
| AI Only | 30-60s | 120-180s | 300-400s | $0.01-0.05 |
| Hybrid | 5-10s | 25-40s | 60-90s | $0.005-0.02 |

### Best Practices

#### 1. Start with Comparison

```bash
# Compare approaches first
POST /api/ai-smart-mapping/compare
```

This helps you decide which approach to use.

#### 2. Use Smart for Initial Generation

```bash
# Generate quickly with smart matching
POST /api/ai-smart-mapping/generate
{
  "useAI": false
}
```

#### 3. Analyze and Enhance if Needed

```bash
# Check coverage
POST /api/ai-smart-mapping/analyze/{partnerId}

# If coverage < 85%, enhance with AI
POST /api/ai-smart-mapping/enhance/{partnerId}
```

#### 4. Monitor Statistics

```bash
# Regular monitoring
GET /api/ai-smart-mapping/stats/{partnerId}
```

### Error Handling

#### Low Coverage Warning

```json
{
  "coveragePercentage": 65.5,
  "suggestions": [
    "Coverage is below 80%. Consider using AI enhancement."
  ]
}
```

**Action:** Use enhance endpoint or regenerate with `useAI: true`

#### Unmapped Critical Fields

```json
{
  "unmappedFieldsList": [
    "$.transaction.amount",
    "$.customer.id"
  ]
}
```

**Action:** Manually add critical field mappings

### Cost Optimization

#### Strategy 1: Smart First, AI for Gaps

```bash
# 1. Generate with smart (free)
POST /api/ai-smart-mapping/generate {"useAI": false}

# 2. Analyze coverage
POST /api/ai-smart-mapping/analyze/{partnerId}

# 3. Only if coverage < 80%, enhance with AI
POST /api/ai-smart-mapping/enhance/{partnerId}
```

**Savings:** 70-80% reduction in AI costs

#### Strategy 2: Batch Processing

```bash
# Process multiple partners in one go
POST /api/ai-smart-mapping/batch-generate
```

**Savings:** Reduced overhead, faster processing

### Monitoring and Alerts

#### Set up alerts for:

1. **Low Coverage** - Coverage < 80%
2. **High Unmapped Count** - Unmapped fields > 20
3. **Transformation Issues** - Missing transformation configs
4. **Performance** - Generation time > 60s

### Integration with Existing Systems

#### With CI/CD Pipeline

```yaml
# .github/workflows/mapping-generation.yml
- name: Generate Mappings
  run: |
    curl -X POST "http://ai-service/api/ai-smart-mapping/batch-generate" \
      -d @partners-config.json
```

#### With Monitoring Tools

```bash
# Export metrics to Prometheus
curl "http://ai-service/api/ai-smart-mapping/stats/{partnerId}" \
  | jq '.coveragePercentage' \
  | curl --data-binary @- http://pushgateway:9091/metrics/job/mapping_coverage
```

### Troubleshooting

#### Issue: Hybrid generation taking too long

**Solution:** Use smart only first, then enhance specific fields

#### Issue: AI enhancement not improving coverage

**Solution:** Check if unmapped fields are in partner JSON

#### Issue: Batch processing failures

**Solution:** Check individual partner configs, process in smaller batches

### Advanced Usage

#### Custom Similarity Threshold

Modify `SmartMappingGenerator.java`:
```java
if (score > 0.7) { // Adjust threshold
    bestMatch = partnerPath;
}
```

#### Selective AI Enhancement

Only enhance specific field patterns:
```java
if (unmappedField.contains("transaction") || 
    unmappedField.contains("payment")) {
    // Use AI for financial fields only
}
```

### API Response Times

| Endpoint | Typical Response Time |
|----------|---------------------|
| /generate (smart) | 2-5s |
| /generate (hybrid) | 5-15s |
| /analyze | 1-2s |
| /enhance | 10-30s |
| /stats | <1s |
| /batch-generate | 5-30s |
| /compare | 15-60s |
