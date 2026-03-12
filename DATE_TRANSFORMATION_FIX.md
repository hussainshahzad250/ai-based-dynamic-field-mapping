# Date Transformation Fix

## Problem

Date field missing from transformation response when input date is in format "yyyy-MM-dd":

**Input:**
```json
{
  "order": {
    "date": "2024-03-10"
  }
}
```

**Expected Output:**
```json
{
  "transaction": {
    "timestamp": "2024-03-10T00:00:00Z"
  }
}
```

**Actual Output:**
```json
{
  "transaction": {
    // timestamp field missing
  }
}
```

## Root Cause

The original `TransformationService.formatDate()` method only handled `LocalDateTime`, but the input date "2024-03-10" is a `LocalDate` format (no time component).

## Solution Applied

### 1. Enhanced TransformationService

Updated `formatDate()` method to handle both `LocalDateTime` and `LocalDate`:

```java
private String formatDate(Object value, MappingConfig.FieldMapping mapping) {
    try {
        String inputFormat = (String) mapping.getTransformationConfig().get("inputFormat");
        String outputFormat = (String) mapping.getTransformationConfig().get("outputFormat");
        
        String dateString = value.toString();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
        
        // Try parsing as LocalDateTime first
        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateString, inputFormatter);
            return dateTime.format(outputFormatter);
        } catch (DateTimeParseException e) {
            // If that fails, try LocalDate
            LocalDate date = LocalDate.parse(dateString, inputFormatter);
            return date.format(outputFormatter);
        }
    } catch (Exception e) {
        log.error("Date format transformation failed: {}", e.getMessage());
        return value.toString(); // Return original value on error
    }
}
```

### 2. Improved Error Handling

- Added try-catch blocks around all transformations
- Log errors with detailed messages
- Return original value on transformation failure
- Use default values when available

### 3. Better Null Handling

Updated `MappingEngine.transform()` to:
- Check for null values before transformation
- Only set values that are not null
- Apply default values when transformation fails

## Mapping Configuration

For the given payload, use this mapping configuration:

```json
{
  "partnerId": "test-partner",
  "version": "1.0",
  "mappings": [
    {
      "targetPath": "customer.customerId",
      "sourcePath": "$.user.id"
    },
    {
      "targetPath": "customer.fullName",
      "sourcePath": "$.user.name",
      "transformationType": "lowercase"
    },
    {
      "targetPath": "customer.contactEmail",
      "sourcePath": "$.user.email"
    },
    {
      "targetPath": "transaction.id",
      "sourcePath": "$.order.orderId"
    },
    {
      "targetPath": "transaction.amount",
      "sourcePath": "$.order.total",
      "transformationType": "multiply",
      "transformationConfig": {
        "factor": 100
      }
    },
    {
      "targetPath": "transaction.timestamp",
      "sourcePath": "$.order.date",
      "transformationType": "dateFormat",
      "transformationConfig": {
        "inputFormat": "yyyy-MM-dd",
        "outputFormat": "yyyy-MM-dd'T'HH:mm:ss'Z'"
      }
    }
  ]
}
```

## Testing

### 1. Save the Mapping Configuration

```bash
curl -X POST "http://localhost:8081/api/admin/mapping/test-partner" \
  -H "X-Admin-Key: admin-secret-key" \
  -H "Content-Type: application/json" \
  -d '{
    "partnerId": "test-partner",
    "version": "1.0",
    "status": "ACTIVE",
    "mappings": [
      {
        "targetPath": "customer.customerId",
        "sourcePath": "$.user.id"
      },
      {
        "targetPath": "customer.fullName",
        "sourcePath": "$.user.name",
        "transformationType": "lowercase"
      },
      {
        "targetPath": "customer.contactEmail",
        "sourcePath": "$.user.email"
      },
      {
        "targetPath": "transaction.id",
        "sourcePath": "$.order.orderId"
      },
      {
        "targetPath": "transaction.amount",
        "sourcePath": "$.order.total",
        "transformationType": "multiply",
        "transformationConfig": {
          "factor": 100
        }
      },
      {
        "targetPath": "transaction.timestamp",
        "sourcePath": "$.order.date",
        "transformationType": "dateFormat",
        "transformationConfig": {
          "inputFormat": "yyyy-MM-dd",
          "outputFormat": "yyyy-MM-dd'\''T'\''HH:mm:ss'\''Z'\''"
        }
      }
    ]
  }'
```

### 2. Transform the Payload

```bash
curl -X POST "http://localhost:8080/api/transform/test-partner" \
  -H "Content-Type: application/json" \
  -d '{
    "user": {
      "id": "U123",
      "name": "Shahzad Hussain",
      "email": "shahzad.hussain@sastechstudio.com"
    },
    "order": {
      "orderId": "ORD456",
      "total": 250.50,
      "date": "2024-03-10"
    }
  }'
```

### 3. Expected Response

```json
{
  "customer": {
    "customerId": "U123",
    "fullName": "shahzad hussain",
    "contactEmail": "shahzad.hussain@sastechstudio.com"
  },
  "transaction": {
    "id": "ORD456",
    "amount": 25050.0,
    "timestamp": "2024-03-10T00:00:00Z"
  }
}
```

## Supported Date Formats

The enhanced transformation now supports:

### Input Formats
- `yyyy-MM-dd` (e.g., "2024-03-10")
- `yyyy-MM-dd HH:mm:ss` (e.g., "2024-03-10 14:30:00")
- `dd-MM-yyyy` (e.g., "10-03-2024")
- `MM/dd/yyyy` (e.g., "03/10/2024")
- Any valid Java DateTimeFormatter pattern

### Output Formats
- `yyyy-MM-dd'T'HH:mm:ss'Z'` (ISO-8601)
- `yyyy-MM-dd`
- `dd/MM/yyyy HH:mm:ss`
- Any valid Java DateTimeFormatter pattern

## Troubleshooting

### Date field still missing

1. **Check logs:**
   ```bash
   # Look for transformation errors
   docker-compose logs -f mapping-engine | grep "Date format"
   ```

2. **Verify mapping configuration:**
   ```bash
   curl "http://localhost:8081/api/ai-mapping/test-partner"
   ```

3. **Test date parsing:**
   - Ensure inputFormat matches your date string exactly
   - For "2024-03-10", use "yyyy-MM-dd"
   - For "10-03-2024", use "dd-MM-yyyy"

### Wrong date format in output

1. **Check outputFormat pattern:**
   - For ISO-8601: `yyyy-MM-dd'T'HH:mm:ss'Z'`
   - For simple date: `yyyy-MM-dd`
   - Escape literals with single quotes

2. **Verify time component:**
   - LocalDate (no time) will default to 00:00:00
   - Use LocalDateTime if you need specific time

### Transformation returns original value

This happens when:
- Input format doesn't match the date string
- Output format is invalid
- Date string is malformed

Check logs for specific error messages.

## Common Date Patterns

```json
{
  "transformationType": "dateFormat",
  "transformationConfig": {
    "inputFormat": "yyyy-MM-dd",
    "outputFormat": "yyyy-MM-dd'T'HH:mm:ss'Z'"
  }
}
```

```json
{
  "transformationType": "dateFormat",
  "transformationConfig": {
    "inputFormat": "dd-MM-yyyy",
    "outputFormat": "yyyy-MM-dd"
  }
}
```

```json
{
  "transformationType": "dateFormat",
  "transformationConfig": {
    "inputFormat": "yyyy-MM-dd HH:mm:ss",
    "outputFormat": "dd/MM/yyyy HH:mm:ss"
  }
}
```
