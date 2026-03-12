package com.fintech.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ai.model.MappingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartMappingGenerator {
    
    private final ObjectMapper objectMapper;
    private final MappingCacheService cacheService;
    
    /**
     * Generate mappings using intelligent field matching for large JSONs
     */
    public MappingConfig generateSmartMapping(String partnerId, String partnerSample, 
                                               String internalSample, String createdBy) {
        try {
            JsonNode partnerNode = objectMapper.readTree(partnerSample);
            JsonNode internalNode = objectMapper.readTree(internalSample);
            
            Map<String, String> partnerFields = extractAllFields(partnerNode, "$");
            Map<String, String> internalFields = extractAllFields(internalNode, "");
            
            log.info("Partner fields: {}, Internal fields: {}", partnerFields.size(), internalFields.size());
            
            List<MappingConfig.FieldMapping> mappings = new ArrayList<>();
            
            // Match fields using multiple strategies
            for (Map.Entry<String, String> internalEntry : internalFields.entrySet()) {
                String internalPath = internalEntry.getKey();
                String internalValue = internalEntry.getValue();
                
                String matchedPartnerPath = findBestMatch(internalPath, internalValue, partnerFields);
                
                if (matchedPartnerPath != null) {
                    MappingConfig.FieldMapping mapping = createMapping(
                        internalPath, matchedPartnerPath, internalValue, 
                        partnerFields.get(matchedPartnerPath)
                    );
                    mappings.add(mapping);
                }
            }
            
            MappingConfig config = new MappingConfig();
            config.setPartnerId(partnerId);
            config.setVersion("1.0");
//            config.setCreatedAt(LocalDateTime.now());
//            config.setUpdatedAt(LocalDateTime.now());
            config.setCreatedBy(createdBy);
            config.setStatus(MappingConfig.ConfigStatus.DRAFT);
            config.setMappings(mappings);
            
            cacheService.cacheMapping(partnerId, config);
            
            log.info("Generated {} mappings for partner {}", mappings.size(), partnerId);
            return config;
            
        } catch (Exception e) {
            log.error("Smart mapping generation failed", e);
            throw new RuntimeException("Failed to generate smart mapping: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract all fields from JSON with their paths and sample values
     */
    private Map<String, String> extractAllFields(JsonNode node, String prefix) {
        Map<String, String> fields = new LinkedHashMap<>();
        extractFieldsRecursive(node, prefix, fields);
        return fields;
    }
    
    private void extractFieldsRecursive(JsonNode node, String currentPath, Map<String, String> fields) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fieldsIterator = node.fields();
            while (fieldsIterator.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIterator.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                
                String newPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
                
                if (fieldValue.isObject()) {
                    extractFieldsRecursive(fieldValue, newPath, fields);
                } else if (fieldValue.isArray() && fieldValue.size() > 0) {
                    // For arrays, analyze first element
                    extractFieldsRecursive(fieldValue.get(0), newPath + "[0]", fields);
                } else if (fieldValue.isValueNode()) {
                    fields.put(newPath, fieldValue.asText());
                }
            }
        }
    }
    
    /**
     * Find best matching partner field for internal field
     */
    private String findBestMatch(String internalPath, String internalValue, 
                                  Map<String, String> partnerFields) {
        
        String internalFieldName = getLastSegment(internalPath);
        
        // Strategy 1: Exact field name match
        for (String partnerPath : partnerFields.keySet()) {
            String partnerFieldName = getLastSegment(partnerPath);
            if (internalFieldName.equalsIgnoreCase(partnerFieldName)) {
                return partnerPath;
            }
        }
        
        // Strategy 2: Similar field name (fuzzy match)
        String bestMatch = null;
        double bestScore = 0.0;
        
        for (String partnerPath : partnerFields.keySet()) {
            String partnerFieldName = getLastSegment(partnerPath);
            double score = calculateSimilarity(internalFieldName, partnerFieldName);
            
            if (score > bestScore && score > 0.7) { // 70% similarity threshold
                bestScore = score;
                bestMatch = partnerPath;
            }
        }
        
        if (bestMatch != null) {
            return bestMatch;
        }
        
        // Strategy 3: Value-based matching (for unique values)
        if (internalValue != null && !internalValue.isEmpty()) {
            for (Map.Entry<String, String> entry : partnerFields.entrySet()) {
                if (internalValue.equals(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create mapping with automatic transformation detection
     */
    private MappingConfig.FieldMapping createMapping(String targetPath, String sourcePath,
                                                      String internalValue, String partnerValue) {
        MappingConfig.FieldMapping mapping = new MappingConfig.FieldMapping();
        mapping.setTargetPath(targetPath);
        mapping.setSourcePath(sourcePath);
        
        // Detect required transformations
        if (internalValue != null && partnerValue != null) {
            
            // Case transformation
            if (internalValue.equals(partnerValue.toUpperCase()) && !partnerValue.equals(partnerValue.toUpperCase())) {
                mapping.setTransformationType("uppercase");
            } else if (internalValue.equals(partnerValue.toLowerCase()) && !partnerValue.equals(partnerValue.toLowerCase())) {
                mapping.setTransformationType("lowercase");
            }
            
            // Date transformation
            else if (isDate(partnerValue) && isDate(internalValue)) {
                String partnerFormat = detectDateFormat(partnerValue);
                String internalFormat = detectDateFormat(internalValue);
                
                if (!partnerFormat.equals(internalFormat)) {
                    mapping.setTransformationType("dateFormat");
                    Map<String, Object> config = new HashMap<>();
                    config.put("inputFormat", partnerFormat);
                    config.put("outputFormat", internalFormat);
                    mapping.setTransformationConfig(config);
                }
            }
            
            // Numeric multiplication (e.g., dollars to cents)
            else if (isNumeric(partnerValue) && isNumeric(internalValue)) {
                double partnerNum = Double.parseDouble(partnerValue);
                double internalNum = Double.parseDouble(internalValue);
                
                if (partnerNum > 0 && Math.abs(internalNum / partnerNum - 100) < 1) {
                    mapping.setTransformationType("multiply");
                    Map<String, Object> config = new HashMap<>();
                    config.put("factor", 100);
                    mapping.setTransformationConfig(config);
                }
            }
        }
        
        return mapping;
    }
    
    private String getLastSegment(String path) {
        if (path.contains(".")) {
            return path.substring(path.lastIndexOf('.') + 1);
        }
        return path.replace("$.", "").replace("[0]", "");
    }
    
    private double calculateSimilarity(String s1, String s2) {
        String longer = s1.toLowerCase();
        String shorter = s2.toLowerCase();
        
        if (longer.length() < shorter.length()) {
            String temp = longer;
            longer = shorter;
            shorter = temp;
        }
        
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }
        
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;
    }
    
    private int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
    
    private boolean isDate(String value) {
        return value.matches("\\d{4}-\\d{2}-\\d{2}.*") || 
               value.matches("\\d{2}-\\d{2}-\\d{4}.*") ||
               value.matches("\\d{2}/\\d{2}/\\d{4}.*");
    }
    
    private String detectDateFormat(String dateValue) {
        if (dateValue.matches("\\d{4}-\\d{2}-\\d{2}T.*")) {
            return "yyyy-MM-dd'T'HH:mm:ss'Z'";
        } else if (dateValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return "yyyy-MM-dd HH:mm:ss";
        } else if (dateValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "yyyy-MM-dd";
        } else if (dateValue.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return "dd-MM-yyyy";
        } else if (dateValue.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return "MM/dd/yyyy";
        }
        return "yyyy-MM-dd";
    }
    
    private boolean isNumeric(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
