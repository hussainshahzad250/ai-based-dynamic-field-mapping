package com.fintech.mapping.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.mapping.model.MappingConfig;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingEngine {
    
    private final ObjectMapper objectMapper;
    private final TransformationService transformationService;
    
    public Map<String, Object> transform(String sourceJson, MappingConfig config) {
        Map<String, Object> result = new HashMap<>();
        
        for (MappingConfig.FieldMapping mapping : config.getMappings()) {
            try {
                Object value = extractValue(sourceJson, mapping);
                
                if (mapping.getTransformationType() != null) {
                    value = transformationService.transform(value, mapping);
                }
                
                setNestedValue(result, mapping.getTargetPath(), value);
            } catch (Exception e) {
                log.warn("Failed to map field {}: {}", mapping.getTargetPath(), e.getMessage());
            }
        }
        
        return result;
    }
    
    private Object extractValue(String json, MappingConfig.FieldMapping mapping) {
        try {
            return JsonPath.read(json, mapping.getSourcePath());
        } catch (Exception e) {
            return mapping.getDefaultValue();
        }
    }
    
    private void setNestedValue(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
        }
        
        current.put(parts[parts.length - 1], value);
    }
}
