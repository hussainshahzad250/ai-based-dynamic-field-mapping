package com.fintech.mapping.service;

import com.fintech.mapping.model.MappingConfig;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TransformationService {
    
    public Object transform(Object value, MappingConfig.FieldMapping mapping) {
        if (value == null) return null;
        
        return switch (mapping.getTransformationType()) {
            case "uppercase" -> value.toString().toUpperCase();
            case "lowercase" -> value.toString().toLowerCase();
            case "dateFormat" -> formatDate(value, mapping);
            case "multiply" -> multiplyValue(value, mapping);
            case "concat" -> concatenate(value, mapping);
            default -> value;
        };
    }
    
    private String formatDate(Object value, MappingConfig.FieldMapping mapping) {
        String inputFormat = (String) mapping.getTransformationConfig().get("inputFormat");
        String outputFormat = (String) mapping.getTransformationConfig().get("outputFormat");
        
        LocalDateTime date = LocalDateTime.parse(value.toString(), DateTimeFormatter.ofPattern(inputFormat));
        return date.format(DateTimeFormatter.ofPattern(outputFormat));
    }
    
    private Object multiplyValue(Object value, MappingConfig.FieldMapping mapping) {
        double multiplier = ((Number) mapping.getTransformationConfig().get("factor")).doubleValue();
        return ((Number) value).doubleValue() * multiplier;
    }
    
    private String concatenate(Object value, MappingConfig.FieldMapping mapping) {
        String prefix = (String) mapping.getTransformationConfig().getOrDefault("prefix", "");
        String suffix = (String) mapping.getTransformationConfig().getOrDefault("suffix", "");
        return prefix + value.toString() + suffix;
    }
}
